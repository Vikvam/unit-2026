package cz.aaa.unit2026.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import unit2026.composeapp.generated.resources.Res
import unit2026.composeapp.generated.resources.nav_debug
import unit2026.composeapp.generated.resources.nav_report
import unit2026.composeapp.generated.resources.nav_settings
import unit2026.composeapp.generated.resources.nav_timer

enum class NavigationDestination(
    val labelRes: StringResource,
    val icon: ImageVector,
    val route: Route,
) {
    Timer(Res.string.nav_timer, Icons.Default.Home, Route.Timer),
    Settings(Res.string.nav_settings, Icons.Default.Settings, Route.Settings),
    Report(Res.string.nav_report, Icons.Default.Info, Route.Report),
    Debug(Res.string.nav_debug, Icons.Default.BugReport, Route.Debug),
    ;

    @Serializable
    sealed interface Route {
        @Serializable data object Timer : Route
        @Serializable data object Settings : Route
        @Serializable data object Report : Route
        @Serializable data object Debug : Route
    }
}
