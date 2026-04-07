package cz.aaa.unit2026.tracking

import cz.aaa.unit2026.util.currentTimeMs
import cz.aaa.unit2026.util.generateSessionId
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.serialization.SerializationException
import java.util.logging.Logger
import kotlin.coroutines.coroutineContext
import kotlin.math.min

private val log: Logger = Logger.getLogger("KtorTrackingClient")

/**
 * JVM (Desktop) implementation of [TrackingClient].
 *
 * - Works offline: [startSession] and [stopSession] update [sessionState] immediately
 *   regardless of connectivity.
 * - Auto-reconnects with exponential backoff (1s → 2s → 4s → … → 32s max).
 * - On reconnect, reconciles with server: server state wins on conflict; local offline
 *   sessions are pushed to the server if no server session is active.
 * - Stopped sessions are preserved in [sessionState] (with [TrackingSession.stoppedAtMs] set)
 *   so the Report screen can display them until a new session begins.
 *
 * Usage:
 * ```kotlin
 * val client = KtorTrackingClient(host = "localhost", port = 8080)
 * // in a coroutine scope:
 * launch { client.run("my-device-id") }
 * // UI:
 * client.sessionState.collect { session -> … }
 * // User action:
 * client.startSession(startedAtMs = now, targetEndAtMs = now + 25.minutes.inWholeMilliseconds)
 * ```
 */
class KtorTrackingClient(
    private val host: String,
    private val port: Int,
) : TrackingClient {

    private val _sessionState = MutableStateFlow<TrackingSession?>(null)
    override val sessionState: StateFlow<TrackingSession?> = _sessionState.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    // Kept as a class field so startSession/stopSession can send while connected.
    private var outgoingChannel: SendChannel<Frame>? = null
    private var running = false

    private val httpClient = HttpClient(CIO) {
        install(WebSockets)
    }

    override suspend fun run(deviceId: String) {
        running = true
        var backoffMs = 1_000L
        while (running && coroutineContext.isActive) {
            try {
                httpClient.webSocket(host = host, port = port, path = "/ws/tracking") {
                    outgoingChannel = this.outgoing
                    _isConnected.value = true
                    backoffMs = 1_000L   // reset on successful connect
                    log.info("[WS] Connected to $host:$port")

                    sendMessage(ClientMessage.Register(deviceId))

                    // Receive SessionState from server for reconciliation
                    val firstFrame = incoming.receive()
                    if (firstFrame is Frame.Text) {
                        val text = firstFrame.readText()
                        log.info("[WS] << $text")
                        reconcile(text)
                    }

                    for (frame in incoming) {
                        if (frame !is Frame.Text) continue
                        val text = frame.readText()
                        log.info("[WS] << $text")
                        handleServerMessage(text)
                    }
                }
            } catch (e: Exception) {
                log.warning("[WS] Connection error: ${e.message}")
            } finally {
                outgoingChannel = null
                _isConnected.value = false
                log.info("[WS] Disconnected from $host:$port")
            }

            if (!running) break
            delay(backoffMs)
            backoffMs = min(backoffMs * 2, 32_000L)
        }
        httpClient.close()
    }

    override suspend fun startSession(startedAtMs: Long, targetEndAtMs: Long) {
        // Clear any expired or finished local session before starting a new one
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
        sendMessage(ClientMessage.SessionStart(startedAtMs, targetEndAtMs))
    }

    override suspend fun stopSession() {
        val current = _sessionState.value ?: return
        _sessionState.value = current.copy(stoppedAtMs = currentTimeMs())
        sendMessage(ClientMessage.SessionStop)
    }

    override fun disconnect() {
        running = false
        httpClient.close()
    }

    private fun handleServerMessage(text: String) {
        val message = try {
            trackingJson.decodeFromString<ServerMessage>(text)
        } catch (_: SerializationException) {
            return   // ignore unrecognised messages — forward-compat
        }
        when (message) {
            is ServerMessage.SessionStarted -> _sessionState.value = message.session
            // Preserve the stopped session so the Report screen can display it.
            is ServerMessage.SessionStopped -> _sessionState.value = message.session
            is ServerMessage.SessionState   -> applyServerState(message.session)
        }
    }

    /**
     * Reconciles server state received on connect.
     * Server wins if it has an active non-expired session.
     * If server has nothing and we have an active local session, push it to the server.
     * Finished/stopped local sessions are left untouched so the Report screen can show them.
     */
    private suspend fun reconcile(text: String) {
        val message = try {
            trackingJson.decodeFromString<ServerMessage>(text)
        } catch (_: SerializationException) {
            return
        }
        if (message !is ServerMessage.SessionState) return

        val serverSession = message.session
        val now = currentTimeMs()

        if (serverSession != null && serverSession.targetEndAtMs > now) {
            // Server has a live session — adopt it
            _sessionState.value = serverSession
        } else {
            // Server has no active session — push active local session if we have one
            val local = _sessionState.value
            if (local != null && local.stoppedAtMs == null && local.targetEndAtMs > now) {
                sendMessage(ClientMessage.SessionStart(local.startedAtMs, local.targetEndAtMs))
            }
            // Do not null out finished/stopped local sessions — Report screen needs them.
        }
    }

    private fun applyServerState(session: TrackingSession?) {
        val now = currentTimeMs()
        if (session != null && session.targetEndAtMs > now) {
            _sessionState.value = session
        }
        // Don't null out local state from a SessionState broadcast — only adopt active sessions.
    }

    private suspend fun sendMessage(message: ClientMessage) {
        val channel = outgoingChannel ?: return
        val text = trackingJson.encodeToString<ClientMessage>(message)
        log.info("[WS] >> $text")
        try { channel.send(Frame.Text(text)) } catch (_: Exception) { /* socket closed */ }
    }
}
