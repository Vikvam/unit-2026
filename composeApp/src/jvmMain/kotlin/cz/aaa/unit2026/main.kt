package cz.aaa.unit2026

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "OpenJetTracks — Monitor Demo",
    ) {
        MonitorDemoApp()
    }
}