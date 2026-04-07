package cz.aaa.unit2026.tracking

import cz.aaa.unit2026.util.currentTimeMs
import cz.aaa.unit2026.util.generateSessionId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Local-only [TrackingClient] for platforms without a server connection (Android, Web).
 *
 * All operations update [sessionState] immediately. No network calls are made.
 * [isConnected] is always false — there is no server.
 */
class OfflineTrackingClient : TrackingClient {

    private val _sessionState = MutableStateFlow<TrackingSession?>(null)
    override val sessionState: StateFlow<TrackingSession?> = _sessionState.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    override suspend fun run(deviceId: String) {
        // No server — nothing to connect to.
    }

    override suspend fun startSession(startedAtMs: Long, targetEndAtMs: Long) {
        _sessionState.value = TrackingSession(
            sessionId = generateSessionId(),
            startedAtMs = startedAtMs,
            targetEndAtMs = targetEndAtMs,
        )
    }

    override suspend fun stopSession() {
        val current = _sessionState.value ?: return
        _sessionState.value = current.copy(stoppedAtMs = currentTimeMs())
    }

    override fun disconnect() = Unit
}
