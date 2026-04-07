package cz.aaa.unit2026.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.aaa.unit2026.ui.theme.OpenJetTracksTheme

enum class TimerState { Running, Paused, Finished, Idle }

/**
 * Circular progress ring with a soft inner fill and gentle glow.
 * Breathes slowly when idle to hint interactivity.
 */
@Composable
fun TimerRing(
    progress: Float,
    state: TimerState,
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 240.dp,
    strokeWidth: Dp = 10.dp,
    subtitle: String? = null,
    labelSize: androidx.compose.ui.unit.TextUnit = 42.sp,
) {
    val ringColor = ringColorFor(state)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val fillColor = ringColor.copy(alpha = 0.07f)

    // Gentle breathing when idle — subtle scale pulse via alpha on the fill
    val breathAlpha = if (state == TimerState.Idle) {
        val transition = rememberInfiniteTransition(label = "breath")
        val alpha by transition.animateFloat(
            initialValue = 0.04f,
            targetValue = 0.12f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "breathAlpha",
        )
        alpha
    } else {
        0.07f
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            val padding = strokeWidth.toPx() / 2
            val arcSize = Size(
                this.size.width - strokeWidth.toPx(),
                this.size.height - strokeWidth.toPx(),
            )
            val topLeft = Offset(padding, padding)
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val radius = (this.size.width - strokeWidth.toPx()) / 2

            // Soft inner fill — makes it feel like a button
            drawCircle(
                color = ringColor.copy(alpha = breathAlpha),
                radius = radius,
                center = center,
            )

            // Track ring
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )

            // Progress arc with gradient
            if (progress > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            ringColor.copy(alpha = 0.6f),
                            ringColor,
                            ringColor,
                        ),
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * progress.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke,
                )
            }

            // Soft outer glow
            drawCircle(
                color = ringColor.copy(alpha = 0.08f),
                radius = radius + strokeWidth.toPx(),
                center = center,
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = labelSize,
                fontWeight = FontWeight.Light,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
private fun ringColorFor(state: TimerState): Color = when (state) {
    TimerState.Running  -> OpenJetTracksTheme.focus.active
    TimerState.Paused   -> OpenJetTracksTheme.focus.warning
    TimerState.Finished -> OpenJetTracksTheme.focus.active
    TimerState.Idle     -> OpenJetTracksTheme.focus.idle
}
