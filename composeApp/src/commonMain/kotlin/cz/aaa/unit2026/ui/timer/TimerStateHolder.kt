package cz.aaa.unit2026.ui.timer

import cz.aaa.unit2026.tracking.TrackingClient
import cz.aaa.unit2026.tracking.TrackingSession
import cz.aaa.unit2026.ui.components.TimerState
import cz.aaa.unit2026.ui.theme.SessionSettings
import cz.aaa.unit2026.util.currentTimeMs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private fun formatDurationMinutes(minutes: Int): String =
    "${minutes.toString().padStart(2, '0')}:00"

data class TimerUiState(
    val timerState: TimerState = TimerState.Idle,
    val progress: Float = 0f,
    val label: String = formatDurationMinutes(SessionSettings.durationMinutes.value),
    val isConnected: Boolean = false,
)

class TimerStateHolder(
    private val client: TrackingClient,
    private val scope: CoroutineScope,
) {
    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private var lastSessionId: String? = null

    init {
        scope.launch {
            client.sessionState.collect { session ->
                if (session != null && session.sessionId != lastSessionId) {
                    // New session adopted — sync duration slider to match the actual session length
                    lastSessionId = session.sessionId
                    val durationMs = session.targetEndAtMs - session.startedAtMs
                    SessionSettings.setDuration((durationMs / 60_000L).toInt().coerceIn(1, 180))
                } else if (session == null) {
                    lastSessionId = null
                }
                _uiState.update { current ->
                    if (session == null) {
                        TimerUiState(
                            label = formatDurationMinutes(SessionSettings.durationMinutes.value),
                            isConnected = current.isConnected,
                        )
                    } else {
                        computeState(session, currentTimeMs()).copy(isConnected = current.isConnected)
                    }
                }
            }
        }

        scope.launch {
            client.isConnected.collect { connected ->
                _uiState.update { it.copy(isConnected = connected) }
            }
        }

        // Tick every 500 ms to keep the displayed label smooth.
        // Skipped when the session is paused — time is frozen at pausedAtMs.
        scope.launch {
            while (true) {
                delay(500L)
                val session = client.sessionState.value ?: continue
                if (session.pausedAtMs != null || session.stoppedAtMs != null) continue
                _uiState.update { current ->
                    computeState(session, currentTimeMs()).copy(isConnected = current.isConnected)
                }
            }
        }
    }

    /** Start a new session with the given duration. No-op if a session is already active. */
    fun onStart(durationMs: Long) {
        scope.launch {
            val now = currentTimeMs()
            client.startSession(startedAtMs = now, targetEndAtMs = now + durationMs)
        }
    }

    fun onStop() {
        scope.launch { client.stopSession() }
    }

    fun onPause() {
        scope.launch { client.pauseSession() }
    }

    fun onResume() {
        scope.launch { client.resumeSession() }
    }

    private fun computeState(session: TrackingSession, now: Long): TimerUiState {
        if (session.stoppedAtMs != null) {
            return TimerUiState(timerState = TimerState.Finished, progress = 1f, label = "Done")
        }

        // When paused, freeze the displayed remaining time at the moment pause was pressed.
        val effectiveNow = session.pausedAtMs ?: now
        val remainingMs = (session.targetEndAtMs - effectiveNow).coerceAtLeast(0L)

        if (remainingMs <= 0L) {
            return TimerUiState(timerState = TimerState.Finished, progress = 1f, label = "00:00")
        }

        val totalMs = (session.targetEndAtMs - session.startedAtMs).coerceAtLeast(1L)
        val progress = ((totalMs - remainingMs).toFloat() / totalMs).coerceIn(0f, 1f)
        return TimerUiState(
            timerState = if (session.pausedAtMs != null) TimerState.Paused else TimerState.Running,
            progress = progress,
            label = formatMs(remainingMs),
        )
    }

    private fun formatMs(ms: Long): String {
        val totalSeconds = (ms / 1_000L).coerceAtLeast(0L)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    }
}
