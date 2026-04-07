package cz.aaa.unit2026.tracking

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
import kotlin.coroutines.coroutineContext
import kotlin.math.min

/**
 * JVM (Android + Desktop) implementation of [TrackingClient].
 *
 * - Works offline: [startSession] and [stopSession] update [sessionState] immediately
 *   regardless of connectivity.
 * - Auto-reconnects with exponential backoff (1s → 2s → 4s → … → 32s max).
 * - On reconnect, reconciles with server: server state wins on conflict; local offline
 *   sessions are pushed to the server if no server session is active.
 *
 * Usage:
 * ```kotlin
 * val client = KtorTrackingClient(host = "192.168.1.x", port = 8080)
 * // in viewModelScope or lifecycleScope:
 * launch { client.run("my-device-id") }
 * // UI:
 * client.sessionState.collect { session -> … }
 * // User action:
 * client.startSession(startedAtMs = now, targetEndAtMs = now + 40.minutes.inWholeMilliseconds)
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
    // Named distinctly to avoid shadowing DefaultClientWebSocketSession.outgoing inside lambdas.
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

                    sendMessage(ClientMessage.Register(deviceId))

                    // Receive SessionState from server for reconciliation
                    val firstFrame = incoming.receive()
                    if (firstFrame is Frame.Text) {
                        reconcile(firstFrame.readText())
                    }

                    for (frame in incoming) {
                        if (frame !is Frame.Text) continue
                        handleServerMessage(frame.readText())
                    }
                }
            } catch (_: Exception) {
                // Connection failed or dropped — fall through to backoff
            } finally {
                outgoingChannel = null
                _isConnected.value = false
            }

            if (!running) break
            delay(backoffMs)
            backoffMs = min(backoffMs * 2, 32_000L)
        }
        httpClient.close()
    }

    override suspend fun startSession(startedAtMs: Long, targetEndAtMs: Long) {
        // Clear any expired local session before starting a new one
        val now = System.currentTimeMillis()
        if (_sessionState.value?.targetEndAtMs?.let { it <= now } == true) {
            _sessionState.value = null
        }

        val session = TrackingSession(
            sessionId = java.util.UUID.randomUUID().toString(),
            startedAtMs = startedAtMs,
            targetEndAtMs = targetEndAtMs,
        )
        _sessionState.value = session
        sendMessage(ClientMessage.SessionStart(startedAtMs, targetEndAtMs))
    }

    override suspend fun stopSession() {
        val current = _sessionState.value ?: return
        _sessionState.value = current.copy(stoppedAtMs = System.currentTimeMillis())
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
            is ServerMessage.SessionStopped -> _sessionState.value = null
            is ServerMessage.SessionState   -> applyServerState(message.session)
        }
    }

    /**
     * Reconciles server state received on connect.
     * Server wins if it has an active non-expired session.
     * If server has nothing and we have a local active session, push it to the server.
     */
    private suspend fun reconcile(text: String) {
        val message = try {
            trackingJson.decodeFromString<ServerMessage>(text)
        } catch (_: SerializationException) {
            return
        }
        if (message !is ServerMessage.SessionState) return

        val serverSession = message.session
        val now = System.currentTimeMillis()

        if (serverSession != null && serverSession.targetEndAtMs > now) {
            // Server has a live session — adopt it
            _sessionState.value = serverSession
        } else {
            // Server has no active session — push local offline session if we have one
            val local = _sessionState.value
            if (local != null && local.stoppedAtMs == null && local.targetEndAtMs > now) {
                sendMessage(ClientMessage.SessionStart(local.startedAtMs, local.targetEndAtMs))
            } else {
                _sessionState.value = null
            }
        }
    }

    private fun applyServerState(session: TrackingSession?) {
        val now = System.currentTimeMillis()
        _sessionState.value = if (session != null && session.targetEndAtMs > now) session else null
    }

    private suspend fun sendMessage(message: ClientMessage) {
        val channel = outgoingChannel ?: return
        val text = trackingJson.encodeToString<ClientMessage>(message)
        try { channel.send(Frame.Text(text)) } catch (_: Exception) { /* socket closed */ }
    }
}
