package cz.aaa.unit2026.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
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

// region — Sage (default) —

private val SageLightScheme = lightColorScheme(
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

private val SageDarkScheme = darkColorScheme(
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

// endregion

// region — Lavender —

private val LavenderLightScheme = lightColorScheme(
    primary = Color(0xFF8B7EC8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE8F5),
    onPrimaryContainer = Color(0xFF2D2650),
    secondary = Color(0xFFC48BB0),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF5E6EE),
    onSecondaryContainer = Color(0xFF3D2232),
    tertiary = Color(0xFFA494D4),
    onTertiary = Color.White,
    background = Color(0xFFF7F5FB),
    onBackground = Color(0xFF2D2B3D),
    surface = Color(0xFFFCFAFF),
    onSurface = Color(0xFF2D2B3D),
    surfaceVariant = Color(0xFFECE8F4),
    onSurfaceVariant = Color(0xFF7A7494),
    error = SoftCoral,
    onError = Color.White,
    outline = Color(0xFF9A94B0),
)

private val LavenderDarkScheme = darkColorScheme(
    primary = Color(0xFFA99ADB),
    onPrimary = Color(0xFF1A1530),
    primaryContainer = Color(0xFF3A3260),
    onPrimaryContainer = Color(0xFFDDD6F0),
    secondary = Color(0xFFD4A0C2),
    onSecondary = Color(0xFF261828),
    secondaryContainer = Color(0xFF4A3248),
    onSecondaryContainer = Color(0xFFF0D8E8),
    tertiary = Color(0xFFBAADE6),
    onTertiary = Color(0xFF1E1840),
    background = Color(0xFF1A1726),
    onBackground = Color(0xFFE2DEF0),
    surface = Color(0xFF242132),
    onSurface = Color(0xFFE2DEF0),
    surfaceVariant = Color(0xFF322E44),
    onSurfaceVariant = Color(0xFFB0AAC8),
    error = SoftCoral,
    onError = Color(0xFF1A0A08),
    outline = Color(0xFF5A5470),
)

// endregion

// region — Rose —

private val RoseLightScheme = lightColorScheme(
    primary = Color(0xFFD4756A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF8E6E3),
    onPrimaryContainer = Color(0xFF3D1D18),
    secondary = Color(0xFFB4917A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF2E6DB),
    onSecondaryContainer = Color(0xFF3A2A1E),
    tertiary = Color(0xFFD49A8E),
    onTertiary = Color.White,
    background = Color(0xFFFBF5F4),
    onBackground = Color(0xFF2D2525),
    surface = Color(0xFFFEFBFA),
    onSurface = Color(0xFF2D2525),
    surfaceVariant = Color(0xFFF4EAE8),
    onSurfaceVariant = Color(0xFF8A7572),
    error = Color(0xFFCC4444),
    onError = Color.White,
    outline = Color(0xFFB09A96),
)

private val RoseDarkScheme = darkColorScheme(
    primary = Color(0xFFE09088),
    onPrimary = Color(0xFF261210),
    primaryContainer = Color(0xFF5A2E28),
    onPrimaryContainer = Color(0xFFF4D0CC),
    secondary = Color(0xFFC8A896),
    onSecondary = Color(0xFF261C14),
    secondaryContainer = Color(0xFF4A382C),
    onSecondaryContainer = Color(0xFFF0DDD2),
    tertiary = Color(0xFFE0AEA4),
    onTertiary = Color(0xFF2A1814),
    background = Color(0xFF211A19),
    onBackground = Color(0xFFECE0DE),
    surface = Color(0xFF2C2322),
    onSurface = Color(0xFFECE0DE),
    surfaceVariant = Color(0xFF3C302E),
    onSurfaceVariant = Color(0xFFC0AAA6),
    error = Color(0xFFE06666),
    onError = Color(0xFF1A0808),
    outline = Color(0xFF604A46),
)

// endregion

// region — Ocean —

private val OceanLightScheme = lightColorScheme(
    primary = Color(0xFF5B98B0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDFF0F5),
    onPrimaryContainer = Color(0xFF142830),
    secondary = Color(0xFF6BABA5),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDFF0EE),
    onSecondaryContainer = Color(0xFF142C2A),
    tertiary = Color(0xFF82B4C4),
    onTertiary = Color.White,
    background = Color(0xFFF3F7F9),
    onBackground = Color(0xFF222D32),
    surface = Color(0xFFFAFCFD),
    onSurface = Color(0xFF222D32),
    surfaceVariant = Color(0xFFE4ECF0),
    onSurfaceVariant = Color(0xFF6A8090),
    error = SoftCoral,
    onError = Color.White,
    outline = Color(0xFF90AAB8),
)

private val OceanDarkScheme = darkColorScheme(
    primary = Color(0xFF78B4CC),
    onPrimary = Color(0xFF0E1E26),
    primaryContainer = Color(0xFF264050),
    onPrimaryContainer = Color(0xFFCCE8F2),
    secondary = Color(0xFF88C4BE),
    onSecondary = Color(0xFF0E2220),
    secondaryContainer = Color(0xFF264844),
    onSecondaryContainer = Color(0xFFCCEEEA),
    tertiary = Color(0xFF9AC8D6),
    onTertiary = Color(0xFF102028),
    background = Color(0xFF161D21),
    onBackground = Color(0xFFD8E4EA),
    surface = Color(0xFF1E272C),
    onSurface = Color(0xFFD8E4EA),
    surfaceVariant = Color(0xFF2A343C),
    onSurfaceVariant = Color(0xFFA0B4C0),
    error = SoftCoral,
    onError = Color(0xFF1A0A08),
    outline = Color(0xFF445864),
)

// endregion

// region — Amber —

private val AmberLightScheme = lightColorScheme(
    primary = Color(0xFFC49B4A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF5EDD8),
    onPrimaryContainer = Color(0xFF302410),
    secondary = Color(0xFFB08060),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF2E6D6),
    onSecondaryContainer = Color(0xFF2E1E10),
    tertiary = Color(0xFFD4B06A),
    onTertiary = Color.White,
    background = Color(0xFFFAF7F0),
    onBackground = Color(0xFF302B22),
    surface = Color(0xFFFDFCF8),
    onSurface = Color(0xFF302B22),
    surfaceVariant = Color(0xFFF0E8DA),
    onSurfaceVariant = Color(0xFF8A7A60),
    error = SoftCoral,
    onError = Color.White,
    outline = Color(0xFFB0A088),
)

private val AmberDarkScheme = darkColorScheme(
    primary = Color(0xFFDAB668),
    onPrimary = Color(0xFF201808),
    primaryContainer = Color(0xFF4A3818),
    onPrimaryContainer = Color(0xFFF0E0B8),
    secondary = Color(0xFFC89878),
    onSecondary = Color(0xFF201408),
    secondaryContainer = Color(0xFF4A3424),
    onSecondaryContainer = Color(0xFFF0D8C4),
    tertiary = Color(0xFFE0C480),
    onTertiary = Color(0xFF241C08),
    background = Color(0xFF1F1B14),
    onBackground = Color(0xFFECE4D6),
    surface = Color(0xFF2A2518),
    onSurface = Color(0xFFECE4D6),
    surfaceVariant = Color(0xFF3A3226),
    onSurfaceVariant = Color(0xFFC0B098),
    error = SoftCoral,
    onError = Color(0xFF1A0A08),
    outline = Color(0xFF5A4E38),
)

// endregion

// region — Slate —

private val SlateLightScheme = lightColorScheme(
    primary = Color(0xFF7889A0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4E9EF),
    onPrimaryContainer = Color(0xFF1E2832),
    secondary = Color(0xFF8A9696),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2EAEA),
    onSecondaryContainer = Color(0xFF1E2828),
    tertiary = Color(0xFF9AA4B0),
    onTertiary = Color.White,
    background = Color(0xFFF5F6F7),
    onBackground = Color(0xFF262A2E),
    surface = Color(0xFFFAFBFC),
    onSurface = Color(0xFF262A2E),
    surfaceVariant = Color(0xFFE8EAEE),
    onSurfaceVariant = Color(0xFF6E7882),
    error = SoftCoral,
    onError = Color.White,
    outline = Color(0xFF9AA4AE),
)

private val SlateDarkScheme = darkColorScheme(
    primary = Color(0xFF94A4B8),
    onPrimary = Color(0xFF10161E),
    primaryContainer = Color(0xFF303C4A),
    onPrimaryContainer = Color(0xFFD0D8E2),
    secondary = Color(0xFFA2B0B0),
    onSecondary = Color(0xFF101818),
    secondaryContainer = Color(0xFF344040),
    onSecondaryContainer = Color(0xFFD2DEDE),
    tertiary = Color(0xFFAEB8C4),
    onTertiary = Color(0xFF141A22),
    background = Color(0xFF181A1E),
    onBackground = Color(0xFFDCE0E4),
    surface = Color(0xFF222528),
    onSurface = Color(0xFFDCE0E4),
    surfaceVariant = Color(0xFF303438),
    onSurfaceVariant = Color(0xFFA8B0B8),
    error = SoftCoral,
    onError = Color(0xFF1A0A08),
    outline = Color(0xFF464E56),
)

// endregion

internal fun colorSchemeFor(theme: ColorTheme, dark: Boolean): ColorScheme = when (theme) {
    ColorTheme.Sage -> if (dark) SageDarkScheme else SageLightScheme
    ColorTheme.Lavender -> if (dark) LavenderDarkScheme else LavenderLightScheme
    ColorTheme.Rose -> if (dark) RoseDarkScheme else RoseLightScheme
    ColorTheme.Ocean -> if (dark) OceanDarkScheme else OceanLightScheme
    ColorTheme.Amber -> if (dark) AmberDarkScheme else AmberLightScheme
    ColorTheme.Slate -> if (dark) SlateDarkScheme else SlateLightScheme
}

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
    colorTheme: ColorTheme = ColorTheme.Sage,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    val colorScheme = colorSchemeFor(colorTheme, darkTheme)
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
