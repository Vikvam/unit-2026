package cz.aaa.unit2026.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import cz.aaa.unit2026.ui.theme.BackgroundTheme
import cz.aaa.unit2026.ui.theme.Mauve
import cz.aaa.unit2026.ui.theme.SageGreen
import cz.aaa.unit2026.ui.theme.CalmBlue
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun AmbientBackground(theme: BackgroundTheme, modifier: Modifier = Modifier) {
    when (theme) {
        BackgroundTheme.Minimal -> { /* nothing */ }
        BackgroundTheme.Stars -> StarsBackground(modifier)
        BackgroundTheme.Forest -> ForestBackground(modifier)
        BackgroundTheme.Ocean -> OceanBackground(modifier)
    }
}

// --- Stars ---

private data class Star(val x: Float, val y: Float, val size: Float, val twinkleSpeed: Int)

@Composable
private fun StarsBackground(modifier: Modifier = Modifier) {
    val stars = remember {
        val rng = Random(42)
        List(80) {
            Star(
                x = rng.nextFloat(),
                y = rng.nextFloat(),
                size = rng.nextFloat() * 2.5f + 0.8f,
                twinkleSpeed = rng.nextInt(3000, 7000),
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "stars")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
        ),
        label = "starTime",
    )

    val starColor = Mauve.copy(alpha = 0.85f)

    Canvas(modifier = modifier.fillMaxSize()) {
        stars.forEach { star ->
            val phase = (time * 10000f / star.twinkleSpeed) * 2f * PI.toFloat()
            val alpha = 0.25f + 0.6f * ((sin(phase) + 1f) / 2f)

            drawCircle(
                color = starColor.copy(alpha = alpha),
                radius = star.size,
                center = Offset(star.x * size.width, star.y * size.height),
            )
        }
    }
}

// --- Forest ---

private data class Leaf(
    val x: Float,
    val speed: Float,
    val size: Float,
    val drift: Float,
    val startY: Float,
    val rotation: Float,
)

@Composable
private fun ForestBackground(modifier: Modifier = Modifier) {
    val leaves = remember {
        val rng = Random(77)
        List(28) {
            Leaf(
                x = rng.nextFloat(),
                speed = rng.nextFloat() * 0.25f + 0.12f,
                size = rng.nextFloat() * 6f + 3f,
                drift = (rng.nextFloat() - 0.5f) * 0.18f,
                startY = rng.nextFloat(),
                rotation = rng.nextFloat() * 2f * PI.toFloat(),
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "forest")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
        ),
        label = "forestTime",
    )

    val leafColor = SageGreen

    Canvas(modifier = modifier.fillMaxSize()) {
        leaves.forEach { leaf ->
            val progress = (leaf.startY + time * leaf.speed) % 1.1f
            val y = progress * size.height
            val x = (leaf.x + sin((progress * 4f * PI).toFloat()) * leaf.drift) * size.width
            val alpha = if (progress > 0.9f) (1f - (progress - 0.9f) / 0.2f) else 1f
            val rot = leaf.rotation + time * 3f

            drawLeafShape(
                center = Offset(x, y),
                leafSize = leaf.size,
                rotation = rot,
                color = leafColor.copy(alpha = (alpha * 0.45f).coerceIn(0f, 0.55f)),
            )
        }
    }
}

private fun DrawScope.drawLeafShape(center: Offset, leafSize: Float, rotation: Float, color: Color) {
    val cosR = cos(rotation)
    val sinR = sin(rotation)

    fun rotated(dx: Float, dy: Float): Offset {
        return Offset(
            center.x + dx * cosR - dy * sinR,
            center.y + dx * sinR + dy * cosR,
        )
    }

    val tip = rotated(0f, -leafSize)
    val bottom = rotated(0f, leafSize)
    val ctrlR1 = rotated(leafSize * 0.7f, -leafSize * 0.3f)
    val ctrlR2 = rotated(leafSize * 0.7f, leafSize * 0.3f)
    val ctrlL1 = rotated(-leafSize * 0.7f, leafSize * 0.3f)
    val ctrlL2 = rotated(-leafSize * 0.7f, -leafSize * 0.3f)

    val path = Path().apply {
        moveTo(tip.x, tip.y)
        cubicTo(ctrlR1.x, ctrlR1.y, ctrlR2.x, ctrlR2.y, bottom.x, bottom.y)
        cubicTo(ctrlL1.x, ctrlL1.y, ctrlL2.x, ctrlL2.y, tip.x, tip.y)
    }
    drawPath(path, color)

    // Leaf vein
    drawLine(
        color = color.copy(alpha = color.alpha * 0.5f),
        start = tip,
        end = bottom,
        strokeWidth = 0.5f,
    )
}

// --- Ocean ---

@Composable
private fun OceanBackground(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "ocean")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
        ),
        label = "oceanTime",
    )

    val waveColor = CalmBlue

    Canvas(modifier = modifier.fillMaxSize()) {
        drawWaveLine(time, yFraction = 0.55f, amplitude = 10f, wavelength = 0.9f, color = waveColor.copy(alpha = 0.10f), strokeWidth = 1.5f)
        drawWaveLine(time + 0.8f, yFraction = 0.62f, amplitude = 14f, wavelength = 0.75f, color = waveColor.copy(alpha = 0.18f), strokeWidth = 2.5f)
        drawWaveLine(time + 1.5f, yFraction = 0.70f, amplitude = 10f, wavelength = 1.0f, color = waveColor.copy(alpha = 0.14f), strokeWidth = 2f)
        drawWaveLine(time + 2.5f, yFraction = 0.78f, amplitude = 18f, wavelength = 0.6f, color = waveColor.copy(alpha = 0.12f), strokeWidth = 1.5f)
        drawWaveLine(time + 3.5f, yFraction = 0.86f, amplitude = 8f, wavelength = 1.2f, color = waveColor.copy(alpha = 0.08f), strokeWidth = 1f)
    }
}

private fun DrawScope.drawWaveLine(
    time: Float,
    yFraction: Float,
    amplitude: Float,
    wavelength: Float,
    color: Color,
    strokeWidth: Float,
) {
    val path = Path()
    val baseY = size.height * yFraction
    val steps = 80
    for (i in 0..steps) {
        val x = (i.toFloat() / steps) * size.width
        val phase = (x / size.width) * 2f * PI.toFloat() / wavelength + time
        val y = baseY + sin(phase) * amplitude

        if (i == 0) path.moveTo(x, y)
        else path.lineTo(x, y)
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth),
    )
}
