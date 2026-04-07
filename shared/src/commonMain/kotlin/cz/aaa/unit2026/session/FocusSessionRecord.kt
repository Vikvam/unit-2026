package cz.aaa.unit2026.session

import kotlinx.serialization.Serializable

@Serializable
data class FocusSessionRecord(
    val id: Long,                               // startTime as ID
    val startTimeMs: Long,
    val endTimeMs: Long,
    val plannedSeconds: Long,
    val completed: Boolean,                     // natural end vs manually stopped
    val distractions: List<DistractionAttempt>,
) {
    val durationSeconds: Long get() = (endTimeMs - startTimeMs) / 1000
    val durationMinutes: Int get() = (durationSeconds / 60).toInt()

    /** 0–100. Each distraction attempt costs 10 points, floor 0. */
    val focusScore: Int get() = (100 - distractions.size * 10).coerceAtLeast(0)
}

@Serializable
data class DistractionAttempt(
    val timestampMs: Long,
    val appId: String,
    val appName: String,
    val matchedRule: String?,
)
