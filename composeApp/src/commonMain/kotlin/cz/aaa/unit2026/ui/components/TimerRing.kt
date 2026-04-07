package cz.aaa.unit2026.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.aaa.unit2026.ui.theme.OpenJetTracksTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class TimerState { Running, Paused, Finished, Idle }

@Composable
fun TimerRing(
    progress: Float,
    state: TimerState,
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 240.dp,
    strokeWidth: Dp = 10.dp,
    subtitle: String? = null,
    labelSize: TextUnit = 42.sp,
) {
    val ringColor by animateColorAsState(
        targetValue = ringColorFor(state),
        animationSpec = tween(600),
        label = "ringColor",
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    // Breathing fill alpha
    val breathAlpha = if (state == TimerState.Idle) {
        val transition = rememberInfiniteTransition(label = "breath")
        val alpha by transition.animateFloat(
            initialValue = 0.03f,
            targetValue = 0.10f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "breathAlpha",
        )
        alpha
    } else {
        0.08f
    }

    // Orbiting dots — rotate continuously when running
    val orbitTransition = rememberInfiniteTransition(label = "orbit")
    val orbitAngle by orbitTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
        ),
        label = "orbitAngle",
    )
    // Dots fade in when running, out when idle
    val dotAlpha by animateFloatAsState(
        targetValue = when (state) {
            TimerState.Running -> 0.7f
            TimerState.Paused -> 0.3f
            else -> 0f
        },
        animationSpec = tween(800),
        label = "dotAlpha",
    )

    // Glow intensity pulses gently when running
    val glowAlpha = if (state == TimerState.Running) {
        val transition = rememberInfiniteTransition(label = "glow")
        val alpha by transition.animateFloat(
            initialValue = 0.06f,
            targetValue = 0.14f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "glowPulse",
        )
        alpha
    } else {
        0.06f
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

            // Outer glow — pulsing
            drawCircle(
                color = ringColor.copy(alpha = glowAlpha),
                radius = radius + strokeWidth.toPx() * 2,
                center = center,
            )

            // Inner fill
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

            // Progress arc
            if (progress > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            ringColor.copy(alpha = 0.5f),
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

                // Dot at the tip of the progress arc
                val tipAngle = (-90f + 360f * progress.coerceIn(0f, 1f)).toDouble() * PI / 180.0
                val tipX = center.x + radius * cos(tipAngle).toFloat()
                val tipY = center.y + radius * sin(tipAngle).toFloat()
                drawCircle(
                    color = ringColor,
                    radius = strokeWidth.toPx() * 0.8f,
                    center = Offset(tipX, tipY),
                )
            }

            // Orbiting dots (3 dots at different offsets)
            if (dotAlpha > 0.01f) {
                val orbitRadius = radius + strokeWidth.toPx() * 1.8f
                for (i in 0 until 3) {
                    val angle = (orbitAngle + i * 120f).toDouble() * PI / 180.0
                    val dx = center.x + orbitRadius * cos(angle).toFloat()
                    val dy = center.y + orbitRadius * sin(angle).toFloat()
                    val dotSize = strokeWidth.toPx() * (0.3f + i * 0.1f)
                    drawCircle(
                        color = ringColor.copy(alpha = dotAlpha * (1f - i * 0.2f)),
                        radius = dotSize,
                        center = Offset(dx, dy),
                    )
                }
            }
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
