package cz.aaa.unit2026

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import cz.aaa.unit2026.blocking.BlockingOverlay
import cz.aaa.unit2026.blocking.DesktopBlockingEnforcer

fun main() = application {
    val enforcer = remember { DesktopBlockingEnforcer() }
    val blocked by enforcer.blockedApp.collectAsState()

    Window(
        onCloseRequest = ::exitApplication,
        title = "OpenJetTracks",
    ) {
        App()
    }

    if (blocked != null) {
        val geo = blocked!!.geometry
        Window(
            onCloseRequest = {},
            title = "",
            state = if (geo != null) WindowState(
                position = WindowPosition((geo.x * geo.scale).dp, (geo.y * geo.scale).dp),
                size = DpSize((geo.width * geo.scale).dp, (geo.height * geo.scale).dp),
            ) else WindowState(),
            undecorated = true,
            alwaysOnTop = true,
            focusable = false,
            resizable = false,
        ) {
            BlockingOverlay(blocked!!)
        }
    }
}
