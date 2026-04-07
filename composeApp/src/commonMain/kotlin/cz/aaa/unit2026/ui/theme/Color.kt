package cz.aaa.unit2026.ui.theme

import androidx.compose.ui.graphics.Color

// --- Brand palette — warm & zen ---

// Neutrals
val Ink = Color(0xFF2D2B3D)
val DuskPurple = Color(0xFF3D3A50)
val Mauve = Color(0xFF9690B0)
val LavenderMist = Color(0xFFE8E5F0)
val Snow = Color(0xFFFAF9FC)
val Cream = Color(0xFFF5F3EE)

// Accents
val SageGreen = Color(0xFF6BAF8D)
val SageGreenDeep = Color(0xFF4A8D6B)
val SoftCoral = Color(0xFFE8847C)
val SoftCoralDeep = Color(0xFFCC635B)
val WarmAmber = Color(0xFFE8B86D)
val CalmBlue = Color(0xFF7BA4C7)
val CalmBlueDeep = Color(0xFF4A7A9E)
val Peach = Color(0xFFF0C4A8)

/**
 * Semantic focus-state colors, resolved per-theme (light/dark).
 */
data class FocusColors(
    val active: Color,
    val activeContainer: Color,
    val distracted: Color,
    val distractedContainer: Color,
    val idle: Color,
    val idleContainer: Color,
    val warning: Color,
)

val LightFocusColors = FocusColors(
    active = SageGreen,
    activeContainer = Color(0xFFDFF0E7),
    distracted = SoftCoral,
    distractedContainer = Color(0xFFFDE8E6),
    idle = CalmBlue,
    idleContainer = Color(0xFFDFEAF4),
    warning = WarmAmber,
)

val DarkFocusColors = FocusColors(
    active = SageGreen,
    activeContainer = Color(0xFF1C3026),
    distracted = SoftCoral,
    distractedContainer = Color(0xFF3A1D1A),
    idle = CalmBlue,
    idleContainer = Color(0xFF1A2636),
    warning = WarmAmber,
)
