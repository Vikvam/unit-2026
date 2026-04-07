package cz.aaa.unit2026.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColorScheme = lightColorScheme(
    primary = SageGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDFF0E7),
    onPrimaryContainer = Ink,
    secondary = CalmBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDFEAF4),
    onSecondaryContainer = Ink,
    tertiary = Peach,
    onTertiary = Ink,
    background = Cream,
    onBackground = Ink,
    surface = Snow,
    onSurface = Ink,
    surfaceVariant = LavenderMist,
    onSurfaceVariant = Mauve,
    error = SoftCoral,
    onError = Color.White,
    outline = Mauve.copy(alpha = 0.5f),
)

private val DarkColorScheme = darkColorScheme(
    primary = SageGreen,
    onPrimary = Color(0xFF0F1F16),
    primaryContainer = Color(0xFF2A4636),
    onPrimaryContainer = Color(0xFFD0ECDB),
    secondary = CalmBlue,
    onSecondary = Color(0xFF0F1A24),
    secondaryContainer = Color(0xFF283E52),
    onSecondaryContainer = Color(0xFFD0E2F0),
    tertiary = Peach,
    onTertiary = Ink,
    background = Color(0xFF161520),
    onBackground = Color(0xFFE0DDE8),
    surface = Color(0xFF201E2C),
    onSurface = Color(0xFFE0DDE8),
    surfaceVariant = Color(0xFF2C2A3A),
    onSurfaceVariant = Color(0xFFA8A2BE),
    error = SoftCoral,
    onError = Color(0xFF1A0A08),
    outline = Color(0xFF4A4660),
)

private val AppTypography = Typography(
    displayLarge = TextStyle(
        fontSize = 48.sp,
        fontWeight = FontWeight.Light,
        letterSpacing = (-0.5).sp,
    ),
    displayMedium = TextStyle(
        fontSize = 36.sp,
        fontWeight = FontWeight.Light,
        letterSpacing = 0.sp,
    ),
    displaySmall = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.Normal,
    ),
    headlineLarge = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    headlineSmall = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleLarge = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Medium,
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.25.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.25.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.4.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp,
    ),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

val LocalFocusColors = staticCompositionLocalOf { LightFocusColors }

@Composable
fun OpenJetTracksTheme(
    themeMode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val focusColors = if (darkTheme) DarkFocusColors else LightFocusColors

    CompositionLocalProvider(
        LocalFocusColors provides focusColors,
        LocalSpacing provides Spacing(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

object OpenJetTracksTheme {
    val focus: FocusColors
        @Composable get() = LocalFocusColors.current

    val spacing: Spacing
        @Composable get() = LocalSpacing.current
}
