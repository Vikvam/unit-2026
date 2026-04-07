package cz.aaa.unit2026.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.aaa.unit2026.ui.theme.OpenJetTracksTheme

/**
 * Compact floating timer bubble for use as a system overlay.
 *
 * A small circle with a filled background, progress ring around the edge,
 * and remaining time in the center. Designed to be hosted by Android's
 * WindowManager or shown as a desktop floating widget.
 *
 * @param progress 0f..1f — fraction of session elapsed
 * @param state    drives the ring color
 * @param label    compact time label (e.g. "12:34")
 * @param size     outer diameter of the bubble
 * @param onClick  called when the bubble is tapped (e.g. return to app)
 */
@Composable
fun TimerBubble(
    progress: Float,
    state: TimerState,
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    onClick: (() -> Unit)? = null,
) {
    val ringColor = bubbleRingColor(state)
    val backgroundColor = bubbleBackgroundColor(state)
    val strokeWidth = 4.dp

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .shadow(elevation = 6.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(backgroundColor),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            val padding = strokeWidth.toPx() / 2
            val arcSize = Size(
                this.size.width - strokeWidth.toPx(),
                this.size.height - strokeWidth.toPx(),
            )
            val topLeft = Offset(padding, padding)

            // Track
            drawArc(
                color = ringColor.copy(alpha = 0.3f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )

            // Progress
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
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

@Composable
private fun bubbleRingColor(state: TimerState): Color = when (state) {
    TimerState.Running  -> OpenJetTracksTheme.focus.active
    TimerState.Paused   -> OpenJetTracksTheme.focus.warning
    TimerState.Finished -> OpenJetTracksTheme.focus.active
    TimerState.Idle     -> OpenJetTracksTheme.focus.idle
}

@Composable
private fun bubbleBackgroundColor(state: TimerState): Color = when (state) {
    TimerState.Running  -> OpenJetTracksTheme.focus.activeContainer
    TimerState.Paused   -> Color(0xFF3A3A3A)
    TimerState.Finished -> OpenJetTracksTheme.focus.activeContainer
    TimerState.Idle     -> OpenJetTracksTheme.focus.idleContainer
}
