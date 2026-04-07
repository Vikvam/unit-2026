package cz.aaa.unit2026.ui.timer

import cz.aaa.unit2026.tracking.TrackingClient
import cz.aaa.unit2026.tracking.TrackingSession
import cz.aaa.unit2026.ui.components.TimerState
import cz.aaa.unit2026.util.currentTimeMs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TimerUiState(
    val timerState: TimerState = TimerState.Idle,
    val progress: Float = 0f,
    val label: String = "25:00",
    val isConnected: Boolean = false,
)

class TimerStateHolder(
    private val client: TrackingClient,
    private val scope: CoroutineScope,
) {
    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            client.sessionState.collect { session ->
                _uiState.update { current ->
                    if (session == null) {
                        TimerUiState(isConnected = current.isConnected)
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
        // On natural expiry, calls stopSession() so the sync chain fires (report, blocking).
        scope.launch {
            while (true) {
                delay(500L)
                val session = client.sessionState.value ?: continue
                if (session.stoppedAtMs != null) continue   // already handled
                if (session.pausedAtMs != null) continue    // time is frozen
                val now = currentTimeMs()
                if (now >= session.targetEndAtMs) {
                    client.stopSession()                    // triggers collect → Finished state
                } else {
                    _uiState.update { current ->
                        computeState(session, now).copy(isConnected = current.isConnected)
                    }
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
