package cz.aaa.unit2026.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import cz.aaa.unit2026.LocalTrackingClient
import cz.aaa.unit2026.blocking.BlockRule
import cz.aaa.unit2026.monitoring.ActiveApp
import cz.aaa.unit2026.monitoring.MonitorStatus
import cz.aaa.unit2026.session.FocusSessionState
import cz.aaa.unit2026.tracking.TrackingClient
import cz.aaa.unit2026.ui.theme.OpenJetTracksTheme
import kotlinx.coroutines.launch

@Composable
fun DebugScreen(modifier: Modifier = Modifier) {
    val spacing = OpenJetTracksTheme.spacing
    val monitorStatus by FocusSessionState.monitorStatus.collectAsState()
    val current by FocusSessionState.currentApp.collectAsState()
    val isRunning by FocusSessionState.isRunning.collectAsState()
    val isStrictMode by FocusSessionState.isStrictMode.collectAsState()
    val blocked by FocusSessionState.blockedApp.collectAsState()
    val blacklist by FocusSessionState.blacklist.collectAsState()
    val blockRules by FocusSessionState.blockRules.collectAsState()
    val seenApps by FocusSessionState.seenApps.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        item {
            Text("Debug", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        }

        // Supabase testing scaffold — wired to Chunk 3 of the Ktor→Supabase migration.
        // Provisional UI; the real linking flow (with warnings, QR codes, copy-to-clipboard)
        // will replace this once the migration is verified end-to-end.
        item { SupabaseStatusCard() }
        item { LinkingCodeCard() }

        if (monitorStatus == MonitorStatus.FAILED) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                ) {
                    Column(
                        modifier = Modifier.padding(spacing.md),
                        verticalArrangement = Arrangement.spacedBy(spacing.xs),
                    ) {
                        Text(
                            "Monitor unavailable",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Text(
                            "Active window monitoring failed to start. A required system dependency may be missing (e.g. qdbus). Check stderr for details.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }
        }

        // Session status
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    Text("Session", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                        StatusChip("Running", isRunning)
                        StatusChip("Strict", isStrictMode)
                    }
                    if (blocked != null) {
                        Text(
                            "Blocking: ${blocked!!.appName} (${blocked!!.appId})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }
        }

        // Current window
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    Text("Current window", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    if (current != null) {
                        AppInfoRows(current!!, blacklist, blockRules)
                    } else {
                        Text("No window detected", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Seen apps log
        item {
            Text(
                "Seen apps (${seenApps.size})",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        items(seenApps.values.toList().sortedBy { it.appId }) { app ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    app.appId,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = if (app.appId in blacklist) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    app.appName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun AppInfoRows(app: ActiveApp, blacklist: Set<String>, blockRules: List<BlockRule>) {
    val spacing = OpenJetTracksTheme.spacing
    val target = "${app.appId} ${app.windowTitle ?: ""}"
    val matchedRule = blockRules.filter { it.enabled }.firstOrNull { rule ->
        runCatching { Regex(rule.pattern, RegexOption.IGNORE_CASE).containsMatchIn(target) }
            .getOrDefault(false)
    }
    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        InfoRow("appId", app.appId)
        InfoRow("appName", app.appName)
        app.windowTitle?.let { InfoRow("title", it) }
        app.category?.let { InfoRow("category", it.toString()) }
        app.geometry?.let { g ->
            InfoRow("geometry", "${g.x}x${g.y} ${g.width}×${g.height} @${g.scale}x")
        }
        InfoRow("blacklisted", if (app.appId in blacklist) "yes" else "no")
        InfoRow("rule match", matchedRule?.label ?: "none")
        InfoRow("target", target)
    }
}

@Composable
private fun InfoRow(key: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(OpenJetTracksTheme.spacing.sm)) {
        Text(
            key,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(OpenJetTracksTheme.spacing.xxl * 2),
        )
        Text(
            value,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StatusChip(label: String, active: Boolean) {
    FilterChip(
        selected = active,
        onClick = {},
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
    )
}

// ---- Supabase testing scaffold ------------------------------------------

@Composable
private fun SupabaseStatusCard() {
    val spacing = OpenJetTracksTheme.spacing
    val client = LocalTrackingClient.current
    val connected by client.isConnected.collectAsState()
    val accountId by client.accountId.collectAsState()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Text(
                "Supabase",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                StatusChip("Connected", connected)
            }
            Text(
                "account: ${accountId?.take(8)?.plus("…") ?: "(none)"}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LinkingCodeCard() {
    val spacing = OpenJetTracksTheme.spacing
    val client: TrackingClient = LocalTrackingClient.current
    val scope = rememberCoroutineScope()

    var generatedCode by remember { mutableStateOf<String?>(null) }
    var generateStatus by remember { mutableStateOf<String?>(null) }
    var redeemInput by remember { mutableStateOf("") }
    var redeemStatus by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Text(
                "Device linking",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            // --- Generate code
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Button(
                    enabled = !busy,
                    onClick = {
                        scope.launch {
                            busy = true
                            runCatching { client.createLinkingCode() }
                                .onSuccess {
                                    generatedCode = it
                                    generateStatus = "Expires in 5 minutes."
                                }
                                .onFailure {
                                    generatedCode = null
                                    generateStatus = "Failed: ${it.message}"
                                }
                            busy = false
                        }
                    },
                ) { Text("Generate linking code") }

                generatedCode?.let { code ->
                    Text(
                        code,
                        style = MaterialTheme.typography.displaySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                generateStatus?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            HorizontalDivider()

            // --- Redeem code
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                OutlinedTextField(
                    value = redeemInput,
                    onValueChange = { redeemInput = it.uppercase() },
                    label = { Text("Linking code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    enabled = !busy && redeemInput.isNotBlank(),
                    onClick = {
                        scope.launch {
                            busy = true
                            runCatching { client.redeemLinkingCode(redeemInput.trim()) }
                                .onSuccess { newAccount ->
                                    redeemStatus = "Linked. New account: ${newAccount.take(8)}…"
                                    redeemInput = ""
                                }
                                .onFailure {
                                    redeemStatus = "Failed: ${it.message}"
                                }
                            busy = false
                        }
                    },
                ) { Text("Redeem code") }
                redeemStatus?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
