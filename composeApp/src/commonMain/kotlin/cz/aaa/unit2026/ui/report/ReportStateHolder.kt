package cz.aaa.unit2026.ui.report

import cz.aaa.unit2026.tracking.TrackingClient
import cz.aaa.unit2026.tracking.TrackingSession
import cz.aaa.unit2026.ui.components.TimelineSegment
import cz.aaa.unit2026.util.currentTimeMs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReportUiState(
    val hasSession: Boolean = false,
    val segments: List<TimelineSegment> = emptyList(),
    val focusPercent: Int = 0,
    val distractedPercent: Int = 0,
    val durationLabel: String = "",
)

/**
 * Observes [TrackingClient.sessionState] and builds the report whenever a session finishes
 * (either stopped early or naturally expired). The last completed session is retained until
 * a new session starts.
 */
class ReportStateHolder(
    private val client: TrackingClient,
    private val scope: CoroutineScope,
) {
    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            client.sessionState.collect { session ->
                if (session == null) return@collect
                val now = currentTimeMs()
                val finished = session.stoppedAtMs != null || session.targetEndAtMs <= now
                if (finished) {
                    _uiState.value = buildReport(session)
                }
            }
        }
    }

    private fun buildReport(session: TrackingSession): ReportUiState {
        val endMs = session.stoppedAtMs ?: session.targetEndAtMs
        val durationMs = (endMs - session.startedAtMs).coerceAtLeast(0L)
        return ReportUiState(
            hasSession = true,
            // Monitoring integration is a future task; for now every session is 100 % focused.
            segments = listOf(TimelineSegment(0f, 1f, focused = true)),
            focusPercent = 100,
            distractedPercent = 0,
            durationLabel = formatDuration(durationMs),
        )
    }

    private fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1_000L
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "${minutes}m ${seconds.toString().padStart(2, '0')}s"
    }
}
