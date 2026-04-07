package cz.aaa.unit2026.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cz.aaa.unit2026.ui.theme.OpenJetTracksTheme

enum class TimerState { Running, Paused, Finished, Idle }

/**
 * Circular progress ring that reflects the current focus session.
 *
 * @param progress 0f..1f — fraction of session elapsed
 * @param state   drives the ring color (green/amber/red/blue)
 * @param label   text shown in the center (e.g. "12:34")
 * @param size    outer diameter of the ring
 * @param strokeWidth thickness of the arc
 */
@Composable
fun TimerRing(
    progress: Float,
    state: TimerState,
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    strokeWidth: Dp = 12.dp,
) {
    val ringColor = ringColorFor(state)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            val padding = strokeWidth.toPx() / 2
            val arcSize = Size(this.size.width - strokeWidth.toPx(), this.size.height - strokeWidth.toPx())
            val topLeft = Offset(padding, padding)

            // Background track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )

            // Progress arc
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )
        }

        Text(
            text = label,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun ringColorFor(state: TimerState): Color = when (state) {
    TimerState.Running  -> OpenJetTracksTheme.focus.active
    TimerState.Paused   -> OpenJetTracksTheme.focus.warning
    TimerState.Finished -> OpenJetTracksTheme.focus.active
    TimerState.Idle     -> OpenJetTracksTheme.focus.idle
}
