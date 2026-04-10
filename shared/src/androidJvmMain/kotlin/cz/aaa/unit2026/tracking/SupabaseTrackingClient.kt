package cz.aaa.unit2026.tracking

import cz.aaa.unit2026.AppStorage
import cz.aaa.unit2026.SupabaseConfig
import cz.aaa.unit2026.util.currentTimeMs
import cz.aaa.unit2026.util.generateSessionId
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.decodeOldRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.logging.Logger
import kotlin.coroutines.coroutineContext
import kotlin.math.min

private val log: Logger = Logger.getLogger("SupabaseTrackingClient")

private const val TABLE = "tracking_sessions"

/** Thrown internally to force the run() loop to reconnect after a linking event. */
private class ReconnectRequested : Exception("Reconnect requested")

/**
 * Supabase-backed implementation of [TrackingClient] for Android + Desktop (JVM).
 *
 * Transport: Postgres row per session, synced via Supabase Realtime postgres_changes.
 * Identity: anonymous auth — each install becomes an `auth.users` row; membership in an
 * `accounts` row (possibly shared with other devices) determines which rows this client sees.
 *
 * Offline semantics:
 *   * All mutations update [sessionState] immediately, regardless of connectivity.
 *   * On reconnect, [reconcile] fetches the authoritative row and resolves conflicts.
 *   * "Server wins" on a live session; exception: if the local client resumed while offline
 *     but the server row is still paused, push the resume so other devices catch up.
 *   * Offline-started sessions are pushed on next connect.
 *   * Stopped/finished sessions are preserved in [sessionState] for the Report screen.
 */
class SupabaseTrackingClient(
    storage: AppStorage,
) : TrackingClient {

    private val _sessionState = MutableStateFlow<TrackingSession?>(null)
    override val sessionState: StateFlow<TrackingSession?> = _sessionState.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _accountId = MutableStateFlow<String?>(null)
    override val accountId: StateFlow<String?> = _accountId.asStateFlow()

    /** Capacity-1 conflated channel that forces the run() loop to reconnect. */
    private val reconnectRequest = Channel<Unit>(capacity = Channel.CONFLATED)

    private val supabase = createSupabaseClient(
        supabaseUrl = SupabaseConfig.URL,
        supabaseKey = SupabaseConfig.ANON_KEY,
    ) {
        install(Auth) {
            sessionManager = AppStorageSessionManager(storage)
            autoLoadFromStorage = true
            autoSaveToStorage = true
            alwaysAutoRefresh = true
        }
        install(Postgrest)
        install(Realtime)
    }

    private var running = false

    override suspend fun run(deviceId: String) {
        // deviceId is ignored — auth.uid() is the identity in the Supabase world.
        running = true
        var backoffMs = 200L
        while (running && coroutineContext.isActive) {
            try {
                ensureAuthSession()
                val account = supabase.postgrest.rpc("ensure_account")
                    .decodeAs<String>()
                _accountId.value = account
                log.info("[Supabase] Account $account")

                coroutineScope {
                    val channel = supabase.channel("tracking-$account")
                    val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                        table = TABLE
                        filter("account_id", FilterOperator.EQ, account)
                    }

                    // Start collecting *before* the SELECT so any writes that land
                    // between the SELECT and first collect are buffered, not lost.
                    val collector = launch {
                        changes.collect { action -> handleRealtimeAction(action) }
                    }

                    // Reconnect watcher — raises a non-cancellation exception that
                    // tears down this coroutineScope and lets the outer loop retry
                    // with a fresh ensure_account() call.
                    val reconnector = launch {
                        reconnectRequest.receive()
                        throw ReconnectRequested()
                    }

                    channel.subscribe(blockUntilSubscribed = true)
                    _isConnected.value = true
                    backoffMs = 200L
                    log.info("[Supabase] Subscribed to $TABLE for account $account")

                    reconcile(account)

                    // Suspend until collector completes, reconnector fires, or scope cancels.
                    try {
                        collector.join()
                    } finally {
                        reconnector.cancel()
                    }
                }
            } catch (e: ReconnectRequested) {
                log.info("[Supabase] Reconnecting after linking")
                backoffMs = 0L   // reconnect immediately
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.warning("[Supabase] Connection error: ${e.message}")
            } finally {
                _isConnected.value = false
                log.info("[Supabase] Disconnected")
            }
            if (!running) break
            if (backoffMs > 0) delay(backoffMs)
            backoffMs = if (backoffMs == 0L) 200L else min(backoffMs * 2, 5_000L)
        }
    }

    override suspend fun startSession(startedAtMs: Long, targetEndAtMs: Long) {
        // Clear any expired or finished local session before starting a new one.
        val now = currentTimeMs()
        val current = _sessionState.value
        if (current != null && (current.stoppedAtMs != null || current.targetEndAtMs <= now)) {
            _sessionState.value = null
        }

        val session = TrackingSession(
            sessionId = generateSessionId(),
            startedAtMs = startedAtMs,
            targetEndAtMs = targetEndAtMs,
        )
        _sessionState.value = session

        val account = _accountId.value ?: return   // offline — reconcile will push on next connect
        try {
            supabase.from(TABLE).insert(session.toInsert(account))
        } catch (e: Exception) {
            // Most likely: partial unique index violation (another device started
            // a session concurrently). Re-fetch and adopt the winner.
            log.warning("[Supabase] INSERT failed, reconciling: ${e.message}")
            runCatching { reconcile(account) }
        }
    }

    override suspend fun stopSession() {
        val current = _sessionState.value ?: return
        _sessionState.value = null
        val account = _accountId.value ?: return
        val stoppedAt = currentTimeMs()
        runCatching {
            supabase.from(TABLE).update(
                update = { set("stopped_at_ms", stoppedAt) },
                request = {
                    filter {
                        eq("id", current.sessionId)
                        eq("account_id", account)
                    }
                },
            )
        }.onFailure { log.warning("[Supabase] stop failed: ${it.message}") }
    }

    override suspend fun pauseSession() {
        val current = _sessionState.value ?: return
        if (current.pausedAtMs != null) return
        val pausedAt = currentTimeMs()
        _sessionState.value = current.copy(pausedAtMs = pausedAt)
        val account = _accountId.value ?: return
        runCatching {
            supabase.from(TABLE).update(
                update = { set("paused_at_ms", pausedAt) },
                request = {
                    filter {
                        eq("id", current.sessionId)
                        eq("account_id", account)
                    }
                },
            )
        }.onFailure { log.warning("[Supabase] pause failed: ${it.message}") }
    }

    override suspend fun resumeSession() {
        val current = _sessionState.value ?: return
        val pausedAt = current.pausedAtMs ?: return
        val extendedTargetEndAtMs = current.targetEndAtMs + (currentTimeMs() - pausedAt)
        _sessionState.value = current.copy(pausedAtMs = null, targetEndAtMs = extendedTargetEndAtMs)
        val account = _accountId.value ?: return
        runCatching {
            supabase.from(TABLE).update(
                update = {
                    set("paused_at_ms", null as Long?)
                    set("target_end_at_ms", extendedTargetEndAtMs)
                },
                request = {
                    filter {
                        eq("id", current.sessionId)
                        eq("account_id", account)
                    }
                },
            )
        }.onFailure { log.warning("[Supabase] resume failed: ${it.message}") }
    }

    override fun disconnect() {
        running = false
        // SupabaseClient.close() is a suspend function; the caller's coroutine
        // scope cancellation will propagate into run()'s coroutineScope and tear
        // down the Realtime channel + Ktor engine.
    }

    override suspend fun createLinkingCode(): String {
        ensureAuthSession()
        // ensure_account guarantees the caller has a membership row before we
        // hand out a code tied to "their" account — useful during cold-start
        // tests where the user opens the Debug screen before run() completes.
        if (_accountId.value == null) {
            _accountId.value = supabase.postgrest.rpc("ensure_account").decodeAs<String>()
        }
        return supabase.postgrest.rpc("create_linking_code").decodeAs<String>()
    }

    override suspend fun redeemLinkingCode(code: String): String {
        ensureAuthSession()
        val newAccount = supabase.postgrest.rpc(
            function = "redeem_linking_code",
            parameters = buildJsonObject { put("p_code", code) },
        ).decodeAs<String>()
        _accountId.value = newAccount
        // Force the run() loop to tear down the current channel and resubscribe
        // under the new account. Conflated channel — multiple trysends collapse.
        reconnectRequest.trySend(Unit)
        return newAccount
    }

    // ---- internals ------------------------------------------------------

    private suspend fun ensureAuthSession() {
        if (supabase.auth.currentSessionOrNull() == null) {
            supabase.auth.signInAnonymously()
            log.info("[Supabase] Anonymous sign-in complete")
        }
    }

    /**
     * Fetches the current active row for the account and reconciles it against
     * the local session state. The *decision* of what to do lives in the pure
     * [SessionReconciler]; this method is only responsible for executing it.
     * Tested via `SessionReconcilerTest` in commonTest.
     */
    private suspend fun reconcile(account: String) {
        val serverRow = supabase.from(TABLE).select {
            filter {
                eq("account_id", account)
                exact("stopped_at_ms", null)
            }
            limit(1)
        }.decodeSingleOrNull<SessionRow>()

        val serverSession = serverRow?.toDomain()
        val local = _sessionState.value
        val now = currentTimeMs()

        when (val decision = SessionReconciler.decide(local, serverSession, now)) {
            is ReconcileDecision.AdoptServer -> {
                _sessionState.value = decision.server
            }
            is ReconcileDecision.AdoptServerAndPushResume -> {
                _sessionState.value = decision.server
                runCatching {
                    supabase.from(TABLE).update(
                        update = {
                            set("paused_at_ms", null as Long?)
                            set("target_end_at_ms", decision.localTargetEndAtMs)
                        },
                        request = {
                            filter {
                                eq("id", decision.server.sessionId)
                                eq("account_id", account)
                            }
                        },
                    )
                }
            }
            is ReconcileDecision.PushLocal -> {
                runCatching {
                    supabase.from(TABLE).insert(decision.local.toInsert(account))
                }.onFailure { log.warning("[Supabase] offline push failed: ${it.message}") }
            }
            ReconcileDecision.NoOp -> Unit
        }
    }

    private fun handleRealtimeAction(action: PostgresAction) {
        val now = currentTimeMs()
        when (action) {
            is PostgresAction.Insert -> {
                val row = runCatching { action.decodeRecord<SessionRow>() }.getOrNull() ?: return
                adoptServerRow(row, now)
            }
            is PostgresAction.Update -> {
                val row = runCatching { action.decodeRecord<SessionRow>() }.getOrNull() ?: return
                adoptServerRow(row, now)
            }
            is PostgresAction.Delete -> {
                val old = runCatching { action.decodeOldRecord<SessionRow>() }.getOrNull() ?: return
                if (_sessionState.value?.sessionId == old.id) {
                    _sessionState.value = null
                }
            }
            is PostgresAction.Select -> Unit   // not subscribed to SELECT events
        }
    }

    private fun adoptServerRow(row: SessionRow, now: Long) {
        val session = row.toDomain()
        if (session.stoppedAtMs != null) {
            // Stop observed — null out iff the local session is this same row.
            // Keep finished local sessions (Report screen).
            val local = _sessionState.value
            if (local?.sessionId == session.sessionId) {
                _sessionState.value = null
            }
            return
        }
        if (session.targetEndAtMs <= now) {
            // Expired; ignore. Clients auto-stop locally on natural expiry.
            return
        }
        _sessionState.value = session
    }
}

// ---- DTOs (data-layer boundary) -----------------------------------------

@Serializable
internal data class SessionRow(
    @SerialName("id") val id: String,
    @SerialName("account_id") val accountId: String,
    @SerialName("started_at_ms") val startedAtMs: Long,
    @SerialName("target_end_at_ms") val targetEndAtMs: Long,
    @SerialName("stopped_at_ms") val stoppedAtMs: Long? = null,
    @SerialName("paused_at_ms") val pausedAtMs: Long? = null,
    @SerialName("label") val label: String? = null,
    // Server-managed; received on SELECT, tolerated by keeping the field nullable.
    // Not included in SessionInsert because explicit nulls on INSERT would override
    // the column's `default now()`.
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
internal data class SessionInsert(
    @SerialName("id") val id: String,
    @SerialName("account_id") val accountId: String,
    @SerialName("started_at_ms") val startedAtMs: Long,
    @SerialName("target_end_at_ms") val targetEndAtMs: Long,
    @SerialName("paused_at_ms") val pausedAtMs: Long? = null,
    @SerialName("label") val label: String? = null,
)

internal fun SessionRow.toDomain() = TrackingSession(
    sessionId = id,
    startedAtMs = startedAtMs,
    targetEndAtMs = targetEndAtMs,
    stoppedAtMs = stoppedAtMs,
    pausedAtMs = pausedAtMs,
    label = label,
)

internal fun TrackingSession.toInsert(accountId: String) = SessionInsert(
    id = sessionId,
    accountId = accountId,
    startedAtMs = startedAtMs,
    targetEndAtMs = targetEndAtMs,
    pausedAtMs = pausedAtMs,
    label = label,
)