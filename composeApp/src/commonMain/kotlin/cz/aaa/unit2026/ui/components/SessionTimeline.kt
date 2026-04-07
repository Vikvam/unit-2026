package cz.aaa.unit2026.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cz.aaa.unit2026.ui.theme.OpenJetTracksTheme

/**
 * A single segment in the session timeline.
 *
 * @param startFraction 0f..1f — where this segment begins relative to total session
 * @param endFraction   0f..1f — where this segment ends
 * @param focused       true = user was on-task, false = distracted
 */
data class TimelineSegment(
    val startFraction: Float,
    val endFraction: Float,
    val focused: Boolean,
)

/**
 * Horizontal bar showing focused vs. distracted time spans within a session.
 * Used in the post-session report screen.
 */
@Composable
fun SessionTimeline(
    segments: List<TimelineSegment>,
    modifier: Modifier = Modifier,
) {
    val focusedColor = OpenJetTracksTheme.focus.active
    val distractedColor = OpenJetTracksTheme.focus.distracted
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Column(modifier = modifier.padding(horizontal = OpenJetTracksTheme.spacing.md)) {
        Text(
            text = "Session timeline",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(OpenJetTracksTheme.spacing.sm))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
        ) {
            val barHeight = size.height
            val radius = CornerRadius(barHeight / 2, barHeight / 2)

            // Background track
            drawRoundRect(
                color = trackColor,
                size = Size(size.width, barHeight),
                cornerRadius = radius,
            )

            // Segments
            for (segment in segments) {
                val x = segment.startFraction.coerceIn(0f, 1f) * size.width
                val w = ((segment.endFraction - segment.startFraction).coerceIn(0f, 1f)) * size.width
                val color: Color = if (segment.focused) focusedColor else distractedColor

                drawRect(
                    color = color,
                    topLeft = Offset(x, 0f),
                    size = Size(w, barHeight),
                )
            }
        }
    }
}
