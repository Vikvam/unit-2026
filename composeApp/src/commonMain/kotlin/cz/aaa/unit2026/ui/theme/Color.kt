package cz.aaa.unit2026.ui.theme

import androidx.compose.ui.graphics.Color

// --- Brand palette ---

val Charcoal = Color(0xFF1E1E2C)
val DeepNavy = Color(0xFF2B2D42)
val SlateGray = Color(0xFF8D99AE)
val CloudWhite = Color(0xFFEDF2F4)
val OffWhite = Color(0xFFF8F9FA)

// Focus state accents
val FocusGreen = Color(0xFF2DC653)
val FocusGreenDark = Color(0xFF1B9E3E)
val WarningAmber = Color(0xFFF4A623)
val DistractedRed = Color(0xFFE63946)
val DistractedRedDark = Color(0xFFC1121F)
val IdleBlue = Color(0xFF457B9D)
val IdleBlueDark = Color(0xFF1D3557)

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
    active = FocusGreen,
    activeContainer = Color(0xFFD4EDDA),
    distracted = DistractedRed,
    distractedContainer = Color(0xFFF8D7DA),
    idle = IdleBlue,
    idleContainer = Color(0xFFD1ECF1),
    warning = WarningAmber,
)

val DarkFocusColors = FocusColors(
    active = FocusGreenDark,
    activeContainer = Color(0xFF14331D),
    distracted = DistractedRedDark,
    distractedContainer = Color(0xFF3B0D11),
    idle = IdleBlueDark,
    idleContainer = Color(0xFF0D1F2D),
    warning = WarningAmber,
)
