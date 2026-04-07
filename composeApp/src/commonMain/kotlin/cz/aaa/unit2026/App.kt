package cz.aaa.unit2026

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import cz.aaa.unit2026.tracking.TrackingClient
import cz.aaa.unit2026.ui.components.AmbientBackground
import cz.aaa.unit2026.ui.screens.HomeScreen
import cz.aaa.unit2026.ui.theme.BackgroundThemeState
import cz.aaa.unit2026.ui.theme.LocaleState
import cz.aaa.unit2026.ui.theme.OpenJetTracksTheme
import cz.aaa.unit2026.ui.theme.ThemeState
import kotlin.random.Random

/**
 * Composition-local carrying the app-wide [TrackingClient].
 * Provided once in [App] and consumed by any screen that needs it.
 */
val LocalTrackingClient = compositionLocalOf<TrackingClient> {
    error("LocalTrackingClient not provided — wrap your composable in App()")
}

@Composable
fun App() {
    val client = remember { createTrackingClient() }
    val themeMode by ThemeState.mode.collectAsState()
    val colorTheme by ThemeState.colorTheme.collectAsState()
    val bgTheme by BackgroundThemeState.theme.collectAsState()
    // Collected so recomposition is triggered when locale changes.
    LocaleState.locale.collectAsState()

    LaunchedEffect(client) {
        client.run(deviceId = "device-${Random.nextInt(10_000)}")
    }

    DisposableEffect(client) {
        onDispose { client.disconnect() }
    }

    CompositionLocalProvider(LocalTrackingClient provides client) {
        OpenJetTracksTheme(themeMode = themeMode, colorTheme = colorTheme) {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                AmbientBackground(theme = bgTheme)
                HomeScreen(modifier = Modifier.safeContentPadding())
            }
        }
    }
}
