package cz.aaa.unit2026.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class NavigationDestination(
    val label: String,
    val icon: ImageVector,
) {
    Timer("Timer", Icons.Default.Home),
    Settings("Settings", Icons.Default.Settings),
    Report("Report", Icons.Default.Info),
}
