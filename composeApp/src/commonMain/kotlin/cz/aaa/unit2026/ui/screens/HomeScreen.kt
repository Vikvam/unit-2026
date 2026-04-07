package cz.aaa.unit2026.ui.screens

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import cz.aaa.unit2026.ui.navigation.NavigationDestination
import org.jetbrains.compose.resources.stringResource

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier) {
        val useNavRail = maxWidth >= 600.dp

        if (useNavRail) {
            ExpandedHome()
        } else {
            CompactHome()
        }
    }
}

/**
 * Phone layout — bottom navigation bar + full-screen content.
 */
@Composable
private fun CompactHome(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        bottomBar = {
            NavigationBar {
                NavigationDestination.entries.forEach { dest ->
                    NavigationBarItem(
                        selected = backStackEntry?.destination?.hasRoute(dest.route::class) == true,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = stringResource(dest.labelRes)) },
                        label = { Text(stringResource(dest.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

/**
 * Desktop / tablet layout — navigation rail on the left + content area.
 */
@Composable
private fun ExpandedHome(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()

    Row(modifier = modifier.fillMaxSize()) {
        NavigationRail {
            NavigationDestination.entries.forEach { dest ->
                NavigationRailItem(
                    selected = backStackEntry?.destination?.hasRoute(dest.route::class) == true,
                    onClick = {
                        navController.navigate(dest.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(dest.icon, contentDescription = stringResource(dest.labelRes)) },
                    label = { Text(stringResource(dest.labelRes)) },
                )
            }
        }
        AppNavHost(
            navController = navController,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun AppNavHost(
    navController: androidx.navigation.NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = NavigationDestination.Timer.route,
        modifier = modifier,
        enterTransition = { fadeIn(tween(250)) },
        exitTransition = { fadeOut(tween(200)) },
    ) {
        composable<NavigationDestination.Route.Timer> {
            TimerScreen()
        }
        composable<NavigationDestination.Route.Settings> {
            SettingsScreen()
        }
        composable<NavigationDestination.Route.Report> {
            ReportScreen()
        }
        composable<NavigationDestination.Route.Debug> {
            DebugScreen()
        }
    }
}
