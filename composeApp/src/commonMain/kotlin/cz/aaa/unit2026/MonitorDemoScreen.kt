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
import cz.aaa.unit2026.blocking.BlockingEnforcer
import cz.aaa.unit2026.blocking.DEFAULT_BLACKLIST
import cz.aaa.unit2026.monitoring.ActiveApp
import cz.aaa.unit2026.monitoring.AppCategory
import cz.aaa.unit2026.monitoring.ForegroundAppMonitor
import kotlinx.coroutines.launch

@Composable
fun MonitorDemoScreen(monitor: ForegroundAppMonitor, enforcer: BlockingEnforcer? = null) {
    val scope = rememberCoroutineScope()
    val events = remember { mutableStateListOf<ActiveApp>() }
    val listState = rememberLazyListState()
    var focusMode by remember { mutableStateOf(false) }
    // appId → appName for everything we've seen
    val seenApps = remember { mutableStateMapOf<String, ActiveApp>() }
    // manually managed blacklist
    val blacklist = remember { mutableStateSetOf<String>().apply { addAll(DEFAULT_BLACKLIST) } }

    val blocked by enforcer?.blockedApp?.collectAsState() ?: remember { mutableStateOf(null) }

    DisposableEffect(monitor) {
        monitor.start()
        val job = scope.launch {
            monitor.activeApp.collect { app ->
                events.add(0, app)
                if (events.size > 100) events.removeLastOrNull()

                seenApps[app.appId] = app

                if (focusMode && enforcer != null && app.appId !in SELF_APP_IDS) {
                    if (app.appId in blacklist) enforcer.block(app)
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
            // Header + focus toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("OpenJetTracks", style = MaterialTheme.typography.headlineSmall)
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

            // Current app card with block/unblock toggle
            val current = events.firstOrNull()?.takeIf { it.appId !in SELF_APP_IDS }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Current", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    if (current != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(current.appName, style = MaterialTheme.typography.titleLarge)
                                current.windowTitle?.let {
                                    Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        current.appId,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    current.category?.let { cat ->
                                        Text(
                                            categoryLabel(cat),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.tertiary,
                                        )
                                    }
                                }
                            }
                            if (enforcer != null) {
                                val isBlocked = current.appId in blacklist
                                FilledTonalButton(
                                    onClick = {
                                        if (isBlocked) blacklist.remove(current.appId)
                                        else blacklist.add(current.appId)
                                    },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = if (isBlocked)
                                            MaterialTheme.colorScheme.errorContainer
                                        else
                                            MaterialTheme.colorScheme.secondaryContainer
                                    )
                                ) {
                                    Text(if (isBlocked) "Unblock" else "Block")
                                }
                            }
                        }
                    } else {
                        Text("Waiting for first event…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Active block status
            if (enforcer != null && blocked != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Blocking", style = MaterialTheme.typography.labelMedium)
                        Text(
                            blocked!!.appName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }

            // Blacklist management
            if (enforcer != null && blacklist.isNotEmpty()) {
                Text("Blacklist (${blacklist.size})", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    blacklist.toList().forEach { appId ->
                        val app = seenApps[appId]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                app?.appName ?: appId,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { blacklist.remove(appId) }) {
                                Text("Remove", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            Text("Event log", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(events) { app ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            formatTime(app.capturedAtMs),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(64.dp)
                        )
                        Text(
                            "${app.appName}  ${app.windowTitle ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (app.appId in blacklist)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

private fun categoryLabel(category: Int) = when (category) {
    AppCategory.GAME         -> "game"
    AppCategory.SOCIAL       -> "social"
    AppCategory.VIDEO        -> "video"
    AppCategory.NEWS         -> "news"
    AppCategory.AUDIO        -> "audio"
    AppCategory.PRODUCTIVITY -> "productivity"
    AppCategory.MAPS         -> "maps"
    AppCategory.IMAGE        -> "image"
    else                     -> "other"
}

private val SELF_APP_IDS = setOf(
    "cz-aaa-unit2026-MainKt",
    "java-lang-Thread",
    "cz.aaa.unit2026",
)

private fun formatTime(ms: Long): String {
    val totalSecs = ms / 1000
    val h = (totalSecs / 3600) % 24
    val m = (totalSecs / 60) % 60
    val s = totalSecs % 60
    return "%02d:%02d:%02d".format(h, m, s)
}
