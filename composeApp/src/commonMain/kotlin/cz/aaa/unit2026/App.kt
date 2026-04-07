package cz.aaa.unit2026

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import cz.aaa.unit2026.session.FocusSessionState
import cz.aaa.unit2026.tracking.TrackingClient
import cz.aaa.unit2026.ui.report.ReportStateHolder
import cz.aaa.unit2026.ui.screens.HomeScreen
import cz.aaa.unit2026.ui.theme.LocaleState
import cz.aaa.unit2026.ui.theme.OpenJetTracksTheme
import cz.aaa.unit2026.ui.theme.ThemeState
import cz.aaa.unit2026.ui.timer.TimerStateHolder
import kotlin.random.Random

/**
 * Composition-local carrying the app-wide [TrackingClient].
 * Provided once in [App] and consumed by any screen that needs it.
 */
val LocalTrackingClient = compositionLocalOf<TrackingClient> {
    error("LocalTrackingClient not provided — wrap your composable in App()")
}

val LocalTimerStateHolder = compositionLocalOf<TimerStateHolder> {
    error("LocalTimerStateHolder not provided — wrap your composable in App()")
}

val LocalReportStateHolder = compositionLocalOf<ReportStateHolder> {
    error("LocalReportStateHolder not provided — wrap your composable in App()")
}

@Composable
fun App() {
    val client = remember { createTrackingClient() }
    val scope = rememberCoroutineScope()
    val timerStateHolder = remember(client, scope) { TimerStateHolder(client, scope) }
    val reportStateHolder = remember(client, scope) { ReportStateHolder(client, scope) }
    val themeMode by ThemeState.mode.collectAsState()
    // Collected so recomposition is triggered when locale changes.
    LocaleState.locale.collectAsState()

    LaunchedEffect(client) {
        client.run(deviceId = "device-${Random.nextInt(10_000)}")
    }

    // Bridge: keep FocusSessionState (blocking / distraction tracking) in sync with
    // the network session owned by TrackingClient.
    LaunchedEffect(client) {
        client.sessionState.collect { session ->
            FocusSessionState.syncFromClient(session)
        }
    }

    DisposableEffect(client) {
        onDispose { client.disconnect() }
    }

    CompositionLocalProvider(
        LocalTrackingClient provides client,
        LocalTimerStateHolder provides timerStateHolder,
        LocalReportStateHolder provides reportStateHolder,
    ) {
        OpenJetTracksTheme(themeMode = themeMode) {
            Surface(modifier = Modifier.fillMaxSize()) {
                HomeScreen(modifier = Modifier.safeContentPadding())
            }
        }
    }
}
