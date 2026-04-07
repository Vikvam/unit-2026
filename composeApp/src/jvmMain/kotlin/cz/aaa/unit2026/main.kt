package cz.aaa.unit2026

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import cz.aaa.unit2026.blocking.DesktopBlockingEnforcer
import cz.aaa.unit2026.monitoring.KdeWaylandAppMonitor
import cz.aaa.unit2026.session.FocusSessionState
import cz.aaa.unit2026.ui.components.BlockingOverlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

// KDE title bar height in physical pixels at 150% scale (~37 logical × 1.5)
private const val TITLE_BAR_PX = 56f

fun main() {
    val scope = CoroutineScope(Dispatchers.Default)
    val enforcer = DesktopBlockingEnforcer()

    FocusSessionState.init(
        monitor = KdeWaylandAppMonitor(scope),
        enforcer = enforcer,
    )
    // Desktop app list: placeholder — desktop blacklist config coming soon
    FocusSessionState.setInstalledApps(emptyList())

    application {
        val blocked by enforcer.blockedApp.collectAsState()
        val remainingSeconds by FocusSessionState.remainingSeconds.collectAsState()
        val remainingLabel = "%d:%02d".format(remainingSeconds / 60, remainingSeconds % 60)

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
                title = "OpenJetTracks-Overlay",
                state = if (geo != null) WindowState(
                    position = WindowPosition((geo.x * geo.scale).dp, (geo.y * geo.scale + TITLE_BAR_PX).dp),
                    size = DpSize((geo.width * geo.scale).dp, (geo.height * geo.scale - TITLE_BAR_PX).dp),
                ) else WindowState(),
                undecorated = true,
                alwaysOnTop = true,
                focusable = false,
                resizable = false,
            ) {
                BlockingOverlay(
                    appName = blocked!!.appName,
                    remainingLabel = remainingLabel,
                )
            }
        }
    }
}
