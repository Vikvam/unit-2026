package cz.aaa.unit2026

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import cz.aaa.unit2026.blocking.BlockingEnforcer
import cz.aaa.unit2026.monitoring.ActiveApp
import cz.aaa.unit2026.monitoring.ForegroundAppMonitor
import kotlinx.coroutines.launch

@Composable
fun MonitorDemoScreen(monitor: ForegroundAppMonitor, enforcer: BlockingEnforcer? = null) {
    val scope = rememberCoroutineScope()
    val events = remember { mutableStateListOf<ActiveApp>() }
    val listState = rememberLazyListState()
    var focusMode by remember { mutableStateOf(false) }

    val blocked by enforcer?.blockedApp?.collectAsState() ?: remember { mutableStateOf(null) }

    DisposableEffect(monitor) {
        monitor.start()
        val job = scope.launch {
            monitor.activeApp.collect { app ->
                events.add(0, app)
                if (events.size > 100) events.removeLastOrNull()
                if (focusMode && enforcer != null && app.appId !in SELF_APP_IDS) {
                    enforcer.block(app)
                }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("OpenJetTracks — Monitor Demo", style = MaterialTheme.typography.headlineSmall)
                if (enforcer != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (focusMode) "Focus ON" else "Focus OFF", style = MaterialTheme.typography.labelMedium)
                        Switch(
                            checked = focusMode,
                            onCheckedChange = { enabled ->
                                focusMode = enabled
                                if (!enabled) enforcer.unblock()
                            }
                        )
                    }
                }
            }

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

            // Blocking status
            if (enforcer != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (blocked != null)
                            MaterialTheme.colorScheme.errorContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Blocking", style = MaterialTheme.typography.labelMedium)
                        Text(
                            blocked?.appId ?: "nothing",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                        )
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

// Resource classes for OpenJetTracks itself — never block our own app
private val SELF_APP_IDS = setOf(
    "cz-aaa-unit2026-MainKt",  // desktop main window
    "java-lang-Thread",         // Compose Hot Reload dev process
    "cz.aaa.unit2026",          // android
)

private fun formatTime(ms: Long): String {
    val totalSecs = ms / 1000
    val h = (totalSecs / 3600) % 24
    val m = (totalSecs / 60) % 60
    val s = totalSecs % 60
    return "%02d:%02d:%02d".format(h, m, s)
}
