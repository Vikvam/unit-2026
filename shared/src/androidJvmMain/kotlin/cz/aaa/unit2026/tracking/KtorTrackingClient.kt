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
import kotlinx.coroutines.isActive
import kotlinx.serialization.SerializationException
import java.util.logging.Logger
import kotlin.coroutines.coroutineContext
import kotlin.math.min

private val log: Logger = Logger.getLogger("KtorTrackingClient")

/**
 * Android + Desktop (JVM) implementation of [TrackingClient].
 *
 * - Works offline: all session mutations update [sessionState] immediately regardless of connectivity.
 * - Auto-reconnects with exponential backoff (200ms → … → 5s max).
 * - On reconnect, reconciles with server: server state wins on conflict; local offline
 *   sessions are pushed to the server if no server session is active.
 * - Exception: if this client resumed while offline but the server is still paused, the resume
 *   is pushed so other devices catch up to the user's intent.
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
    private val secure: Boolean = false,
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
        var backoffMs = 200L
        while (running && coroutineContext.isActive) {
            try {
                httpClient.webSocket(host = host, port = port, path = "/ws/tracking", request = {
                    url.protocol = if (secure) io.ktor.http.URLProtocol.WSS else io.ktor.http.URLProtocol.WS
                }) {
                    outgoingChannel = this.outgoing
                    _isConnected.value = true
                    backoffMs = 200L   // reset on successful connect
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
            backoffMs = min(backoffMs * 2, 5_000L)
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
        if (_sessionState.value == null) return
        _sessionState.value = null
        sendMessage(ClientMessage.SessionStop)
    }

    override suspend fun pauseSession() {
        val current = _sessionState.value ?: return
        if (current.pausedAtMs != null) return
        _sessionState.value = current.copy(pausedAtMs = currentTimeMs())
        sendMessage(ClientMessage.SessionPause)
    }

    override suspend fun resumeSession() {
        val current = _sessionState.value ?: return
        val pausedAt = current.pausedAtMs ?: return
        val extendedTargetEndAtMs = current.targetEndAtMs + (currentTimeMs() - pausedAt)
        _sessionState.value = current.copy(pausedAtMs = null, targetEndAtMs = extendedTargetEndAtMs)
        sendMessage(ClientMessage.SessionResume(extendedTargetEndAtMs))
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
            is ServerMessage.SessionStarted  -> _sessionState.value = message.session
            // Null out on stop so the timer resets to Idle; history is kept in FocusSessionState.
            is ServerMessage.SessionStopped  -> _sessionState.value = null
            is ServerMessage.SessionPaused   -> _sessionState.value = message.session
            is ServerMessage.SessionResumed  -> _sessionState.value = message.session
            is ServerMessage.SessionState    -> applyServerState(message.session)
        }
    }

    /**
     * Reconciles server state received on connect.
     * Server wins if it has an active non-expired session.
     * Exception: if this client resumed while offline but the server is still paused, the resume
     * is pushed so other devices are notified and the server catches up.
     * If server has nothing and we have an active local session, push it to the server,
     * including its pause state if paused.
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
            // Server has a live session — adopt it.
            // Exception: if we resumed while offline but server is still paused, push the resume
            // so other devices are notified and the server state catches up to our intent.
            val local = _sessionState.value
            _sessionState.value = serverSession
            if (local != null && local.pausedAtMs == null && serverSession.pausedAtMs != null) {
                // We resumed offline but server is still paused — push our resumed state.
                // Use local.targetEndAtMs which already includes the pause extension we computed offline.
                sendMessage(ClientMessage.SessionResume(local.targetEndAtMs))
            }
        } else {
            // Server has no active session — push active local session if we have one.
            val local = _sessionState.value
            if (local != null && local.stoppedAtMs == null && local.targetEndAtMs > now) {
                sendMessage(ClientMessage.SessionStart(local.startedAtMs, local.targetEndAtMs))
                // Also push pause state so other devices see the correct state.
                if (local.pausedAtMs != null) {
                    sendMessage(ClientMessage.SessionPause)
                }
            }
            // Do not null out finished/stopped local sessions — Report screen needs them.
        }
    }

    private fun applyServerState(session: TrackingSession?) {
        val now = currentTimeMs()
        if (session != null && session.targetEndAtMs > now) {
            _sessionState.value = session
        } else {
            // Server has no active session. Only null out if our local session is also
            // inactive — otherwise we may have a valid offline session that the server
            // doesn't know about yet (e.g. SessionState(null) sent as rejection feedback).
            val local = _sessionState.value
            if (local == null || local.stoppedAtMs != null || local.targetEndAtMs <= now) {
                _sessionState.value = null
            }
        }
    }

    private suspend fun sendMessage(message: ClientMessage) {
        val channel = outgoingChannel ?: return
        val text = trackingJson.encodeToString<ClientMessage>(message)
        log.info("[WS] >> $text")
        try { channel.send(Frame.Text(text)) } catch (_: Exception) { /* socket closed */ }
    }
}
