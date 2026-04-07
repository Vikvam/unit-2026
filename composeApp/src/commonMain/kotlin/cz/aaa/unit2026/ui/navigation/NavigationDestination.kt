package cz.aaa.unit2026.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource
import unit2026.composeapp.generated.resources.Res
import unit2026.composeapp.generated.resources.nav_debug
import unit2026.composeapp.generated.resources.nav_report
import unit2026.composeapp.generated.resources.nav_settings
import unit2026.composeapp.generated.resources.nav_timer

enum class NavigationDestination(
    val labelRes: StringResource,
    val icon: ImageVector,
) {
    Timer(Res.string.nav_timer, Icons.Default.Home),
    Settings(Res.string.nav_settings, Icons.Default.Settings),
    Report(Res.string.nav_report, Icons.Default.Info),
    Debug(Res.string.nav_debug, Icons.Default.BugReport),
}
