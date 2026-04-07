package cz.aaa.unit2026.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import cz.aaa.unit2026.blocking.InstalledApp
import cz.aaa.unit2026.monitoring.AppCategory
import cz.aaa.unit2026.session.FocusSessionState
import cz.aaa.unit2026.ui.theme.AppLocale
import cz.aaa.unit2026.ui.theme.LocaleState
import cz.aaa.unit2026.ui.theme.OpenJetTracksTheme
import cz.aaa.unit2026.ui.theme.ThemeMode
import cz.aaa.unit2026.ui.theme.ThemeState
import cz.aaa.unit2026.ui.util.decodeImageBitmap
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import unit2026.composeapp.generated.resources.Res
import unit2026.composeapp.generated.resources.settings_blocked_apps
import unit2026.composeapp.generated.resources.settings_blocked_apps_count
import unit2026.composeapp.generated.resources.settings_blocked_apps_desc
import unit2026.composeapp.generated.resources.settings_blocked_apps_empty
import unit2026.composeapp.generated.resources.settings_blocked_apps_in_category
import unit2026.composeapp.generated.resources.settings_blocked_apps_search
import unit2026.composeapp.generated.resources.settings_language
import unit2026.composeapp.generated.resources.settings_language_desc
import unit2026.composeapp.generated.resources.settings_theme
import unit2026.composeapp.generated.resources.settings_theme_desc
import unit2026.composeapp.generated.resources.settings_title
import unit2026.composeapp.generated.resources.theme_dark
import unit2026.composeapp.generated.resources.theme_light
import unit2026.composeapp.generated.resources.theme_system

private val ThemeMode.labelRes: StringResource
    get() = when (this) {
        ThemeMode.System -> Res.string.theme_system
        ThemeMode.Light -> Res.string.theme_light
        ThemeMode.Dark -> Res.string.theme_dark
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val spacing = OpenJetTracksTheme.spacing
    val currentMode by ThemeState.mode.collectAsState()
    val currentLocale by LocaleState.locale.collectAsState()
    val blacklist by FocusSessionState.blacklist.collectAsState()
    val blockRules by FocusSessionState.blockRules.collectAsState()
    val seenApps by FocusSessionState.seenApps.collectAsState()
    val installedApps by FocusSessionState.installedApps.collectAsState()
    var newRuleLabel by remember { mutableStateOf("") }
    var newRulePattern by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Text(
            text = stringResource(Res.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
        )

        // --- Appearance ---
        SettingsCard {
            SectionLabel(stringResource(Res.string.settings_theme))
            Text(
                text = stringResource(Res.string.settings_theme_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(spacing.sm))

            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = currentMode == mode,
                        onClick = { ThemeState.setMode(mode) },
                        label = { Text(stringResource(mode.labelRes)) },
                    )
                }
            }

            Spacer(Modifier.height(spacing.lg))

            SectionLabel(stringResource(Res.string.settings_language))
            Text(
                text = stringResource(Res.string.settings_language_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(spacing.sm))

            var languageExpanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = languageExpanded,
                onExpandedChange = { languageExpanded = it },
            ) {
                OutlinedTextField(
                    value = "${currentLocale.flag}  ${currentLocale.displayName}",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageExpanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = languageExpanded,
                    onDismissRequest = { languageExpanded = false },
                ) {
                    AppLocale.entries.forEach { locale ->
                        DropdownMenuItem(
                            text = { Text("${locale.flag}  ${locale.displayName}") },
                            onClick = {
                                LocaleState.setLocale(locale)
                                languageExpanded = false
                            },
                        )
                    }
                }
            }
        }

        // --- Block rules (regex — for desktop browser titles) ---
        SettingsCard {
            SectionLabel("Block rules")
            Text(
                "Matched against app name + window title. Useful for blocking sites inside a browser.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(spacing.sm))

            blockRules.forEach { rule ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(rule.label, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            rule.pattern,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = rule.enabled,
                            onCheckedChange = { FocusSessionState.toggleBlockRule(rule) },
                        )
                        TextButton(onClick = { FocusSessionState.removeBlockRule(rule) }) {
                            Text("✕", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            Spacer(Modifier.height(spacing.sm))

            // Add custom rule
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = newRuleLabel,
                    onValueChange = { newRuleLabel = it },
                    label = { Text("Label") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = newRulePattern,
                    onValueChange = { newRulePattern = it },
                    label = { Text("Regex") },
                    modifier = Modifier.weight(2f),
                    singleLine = true,
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace),
                )
                Button(
                    onClick = {
                        if (newRuleLabel.isNotBlank() && newRulePattern.isNotBlank()) {
                            FocusSessionState.addBlockRule(
                                cz.aaa.unit2026.blocking.BlockRule(
                                    label = newRuleLabel.trim(),
                                    pattern = newRulePattern.trim(),
                                )
                            )
                            newRuleLabel = ""
                            newRulePattern = ""
                        }
                    },
                ) { Text("+") }
            }
        }

        // --- Blocked apps ---
        SettingsCard {
            SectionLabel(stringResource(Res.string.settings_blocked_apps))
            Text(
                text = stringResource(Res.string.settings_blocked_apps_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(spacing.sm))

            // On Android: use installedApps. On Desktop: fall back to seenApps.
            if (installedApps.isNotEmpty()) {
                BlockedAppsList(
                    apps = installedApps,
                    blacklist = blacklist,
                    onToggle = { appId, blocked ->
                        if (blocked) FocusSessionState.addToBlacklist(appId)
                        else FocusSessionState.removeFromBlacklist(appId)
                    },
                )
            } else if (seenApps.isNotEmpty()) {
                // Desktop fallback — flat list from seen apps
                seenApps.values.sortedBy { it.appId }.forEach { app ->
                    val isBlocked = app.appId in blacklist
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isBlocked) FocusSessionState.removeFromBlacklist(app.appId)
                                else FocusSessionState.addToBlacklist(app.appId)
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(app.appName, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                app.appId,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Checkbox(
                            checked = isBlocked,
                            onCheckedChange = { checked ->
                                if (checked) FocusSessionState.addToBlacklist(app.appId)
                                else FocusSessionState.removeFromBlacklist(app.appId)
                            },
                        )
                    }
                }
            } else {
                Text(
                    text = stringResource(Res.string.settings_blocked_apps_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// --- Blocked apps list with search, categories, icons ---

@Composable
private fun BlockedAppsList(
    apps: List<InstalledApp>,
    blacklist: Set<String>,
    onToggle: (appId: String, blocked: Boolean) -> Unit,
) {
    val spacing = OpenJetTracksTheme.spacing
    var searchQuery by remember { mutableStateOf("") }

    OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text(stringResource(Res.string.settings_blocked_apps_search)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(Modifier.height(spacing.sm))

    val blockedCount = apps.count { it.appId in blacklist }
    Text(
        text = stringResource(Res.string.settings_blocked_apps_count, blockedCount, apps.size),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(Modifier.height(spacing.sm))

    val filtered = if (searchQuery.isBlank()) apps
    else apps.filter { it.appName.contains(searchQuery, ignoreCase = true) }

    val grouped = filtered.groupBy { it.category }
    val sortedCategories = AppCategory.DISPLAY_ORDER.filter { it in grouped }

    sortedCategories.forEach { category ->
        val categoryApps = grouped[category] ?: return@forEach
        val sorted = categoryApps.sortedWith(
            compareByDescending<InstalledApp> { it.appId in blacklist }
                .thenBy { it.appName },
        )

        CategoryGroup(
            categoryName = AppCategory.displayName(category),
            apps = sorted,
            blacklist = blacklist,
            onToggle = onToggle,
            onToggleAll = { block ->
                val ids = sorted.map { it.appId }.toSet()
                if (block) FocusSessionState.addAllToBlacklist(ids)
                else FocusSessionState.removeAllFromBlacklist(ids)
            },
        )
    }
}

@Composable
private fun CategoryGroup(
    categoryName: String,
    apps: List<InstalledApp>,
    blacklist: Set<String>,
    onToggle: (appId: String, blocked: Boolean) -> Unit,
    onToggleAll: (block: Boolean) -> Unit,
) {
    val spacing = OpenJetTracksTheme.spacing
    val blockedInCategory = apps.count { it.appId in blacklist }
    val allBlocked = blockedInCategory == apps.size
    var expanded by remember { mutableStateOf(blockedInCategory > 0) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = allBlocked,
            onCheckedChange = { onToggleAll(it) },
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = categoryName,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            if (blockedInCategory > 0 && !allBlocked) {
                Text(
                    text = stringResource(Res.string.settings_blocked_apps_in_category, blockedInCategory),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Icon(
            imageVector = if (expanded) Icons.Default.KeyboardArrowUp
            else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    AnimatedVisibility(visible = expanded) {
        Column {
            apps.forEach { app ->
                val isBlocked = app.appId in blacklist
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggle(app.appId, !isBlocked) }
                        .padding(vertical = spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    val icon = app.iconBytes?.let { decodeImageBitmap(it) }
                    if (icon != null) {
                        Image(
                            bitmap = icon,
                            contentDescription = null,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp)),
                        )
                    } else {
                        Spacer(Modifier.size(32.dp))
                    }

                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Checkbox(
                        checked = isBlocked,
                        onCheckedChange = { onToggle(app.appId, it) },
                    )
                }
            }
        }
    }
}

// --- Helpers ---

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(OpenJetTracksTheme.spacing.md)) {
            content()
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}
