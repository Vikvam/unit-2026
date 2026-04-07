package cz.aaa.unit2026

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import cz.aaa.unit2026.ui.screens.HomeScreen
import cz.aaa.unit2026.ui.theme.LocaleState
import cz.aaa.unit2026.ui.theme.OpenJetTracksTheme
import cz.aaa.unit2026.ui.theme.ThemeState

@Composable
@Preview
fun App() {
    val themeMode by ThemeState.mode.collectAsState()
    // Collected so recomposition is triggered when locale changes.
    // The platform locale is applied synchronously in LocaleState.setLocale().
    LocaleState.locale.collectAsState()

    OpenJetTracksTheme(themeMode = themeMode) {
        Surface(modifier = Modifier.fillMaxSize()) {
            HomeScreen(modifier = Modifier.safeContentPadding())
        }
    }
}
