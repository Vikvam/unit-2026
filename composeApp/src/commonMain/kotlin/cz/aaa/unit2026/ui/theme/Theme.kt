package cz.aaa.unit2026.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = DeepNavy,
    onPrimary = CloudWhite,
    primaryContainer = Color(0xFFD6DCEA),
    onPrimaryContainer = Charcoal,
    secondary = IdleBlue,
    onSecondary = Color.White,
    background = OffWhite,
    onBackground = Charcoal,
    surface = Color.White,
    onSurface = Charcoal,
    error = DistractedRed,
    onError = Color.White,
)

private val DarkColorScheme = darkColorScheme(
    primary = SlateGray,
    onPrimary = Charcoal,
    primaryContainer = DeepNavy,
    onPrimaryContainer = CloudWhite,
    secondary = IdleBlueDark,
    onSecondary = CloudWhite,
    background = Charcoal,
    onBackground = CloudWhite,
    surface = DeepNavy,
    onSurface = CloudWhite,
    error = DistractedRedDark,
    onError = CloudWhite,
)

val LocalFocusColors = staticCompositionLocalOf { LightFocusColors }

@Composable
fun OpenJetTracksTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val focusColors = if (darkTheme) DarkFocusColors else LightFocusColors

    CompositionLocalProvider(
        LocalFocusColors provides focusColors,
        LocalSpacing provides Spacing(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
}

/**
 * Convenience accessors so screens can write `OpenJetTracksTheme.focus.active`
 * instead of reaching for `LocalFocusColors.current` directly.
 */
object OpenJetTracksTheme {
    val focus: FocusColors
        @Composable get() = LocalFocusColors.current

    val spacing: Spacing
        @Composable get() = LocalSpacing.current
}
