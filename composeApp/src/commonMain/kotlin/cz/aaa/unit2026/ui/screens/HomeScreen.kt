package cz.aaa.unit2026.ui.screens

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cz.aaa.unit2026.ui.navigation.NavigationDestination
import org.jetbrains.compose.resources.stringResource

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier) {
        val useNavRail = maxWidth >= 600.dp

    var selected by rememberSaveable { mutableStateOf(NavigationDestination.Timer) }

        if (useNavRail) {
            ExpandedHome(selected = selected, onSelect = { selected = it })
        } else {
            CompactHome(selected = selected, onSelect = { selected = it })
        }
    }
}

/**
 * Phone layout — bottom navigation bar + full-screen content.
 */
@Composable
private fun CompactHome(
    selected: NavigationDestination,
    onSelect: (NavigationDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationDestination.entries.forEach { dest ->
                    NavigationBarItem(
                        selected = selected == dest,
                        onClick = { onSelect(dest) },
                        icon = { Icon(dest.icon, contentDescription = stringResource(dest.labelRes)) },
                        label = { Text(stringResource(dest.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        ScreenContent(
            destination = selected,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

/**
 * Desktop / tablet layout — navigation rail on the left + content area.
 */
@Composable
private fun ExpandedHome(
    selected: NavigationDestination,
    onSelect: (NavigationDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxSize()) {
        NavigationRail {
            NavigationDestination.entries.forEach { dest ->
                NavigationRailItem(
                    selected = selected == dest,
                    onClick = { onSelect(dest) },
                    icon = { Icon(dest.icon, contentDescription = stringResource(dest.labelRes)) },
                    label = { Text(stringResource(dest.labelRes)) },
                )
            }
        }
        ScreenContent(
            destination = selected,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ScreenContent(
    destination: NavigationDestination,
    modifier: Modifier = Modifier,
) {
    when (destination) {
        NavigationDestination.Timer -> TimerScreen(modifier = modifier)
        NavigationDestination.Settings -> SettingsScreen(modifier = modifier)
        NavigationDestination.Report -> ReportScreen(modifier = modifier)
        NavigationDestination.Debug -> DebugScreen(modifier = modifier)
    }
}
