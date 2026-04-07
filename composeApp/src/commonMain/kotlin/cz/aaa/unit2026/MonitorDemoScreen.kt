package cz.aaa.unit2026

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cz.aaa.unit2026.monitoring.ActiveApp
import cz.aaa.unit2026.monitoring.ForegroundAppMonitor
import kotlinx.coroutines.launch

@Composable
fun MonitorDemoScreen(monitor: ForegroundAppMonitor) {
    val scope = rememberCoroutineScope()
    val events = remember { mutableStateListOf<ActiveApp>() }
    val listState = rememberLazyListState()

    DisposableEffect(monitor) {
        monitor.start()
        val job = scope.launch {
            monitor.activeApp.collect { app ->
                events.add(0, app)
                if (events.size > 100) events.removeLastOrNull()
            }
        }
        onDispose {
            job.cancel()
            monitor.stop()
        }
    }

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("OpenJetTracks — Monitor Demo", style = MaterialTheme.typography.headlineSmall)

            val current = events.firstOrNull()
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Current", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    if (current != null) {
                        Text(current.appName, style = MaterialTheme.typography.titleLarge)
                        current.windowTitle?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(
                            "appId: ${current.appId}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text("Waiting for first event…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Text("Event log", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(events) { app ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            formatTime(app.capturedAtMs),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(64.dp)
                        )
                        Text(
                            "${app.appId}  ${app.windowTitle ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSecs = ms / 1000
    val h = (totalSecs / 3600) % 24
    val m = (totalSecs / 60) % 60
    val s = totalSecs % 60
    return "%02d:%02d:%02d".format(h, m, s)
}
