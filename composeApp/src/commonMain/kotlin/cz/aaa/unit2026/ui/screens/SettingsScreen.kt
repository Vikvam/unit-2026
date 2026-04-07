package cz.aaa.unit2026.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cz.aaa.unit2026.blocking.InstalledApp
import cz.aaa.unit2026.monitoring.AppCategory
import cz.aaa.unit2026.session.FocusSessionState
import cz.aaa.unit2026.ui.theme.AppLocale
import cz.aaa.unit2026.ui.theme.LocaleState
import cz.aaa.unit2026.ui.theme.OpenJetTracksTheme
import cz.aaa.unit2026.ui.theme.ThemeMode
import cz.aaa.unit2026.ui.theme.ThemeState
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import unit2026.composeapp.generated.resources.Res
import unit2026.composeapp.generated.resources.settings_language
import unit2026.composeapp.generated.resources.settings_language_desc
import unit2026.composeapp.generated.resources.settings_theme
import unit2026.composeapp.generated.resources.settings_theme_desc
import unit2026.composeapp.generated.resources.settings_blocked_apps
import unit2026.composeapp.generated.resources.settings_blocked_apps_count
import unit2026.composeapp.generated.resources.settings_blocked_apps_desc
import unit2026.composeapp.generated.resources.settings_blocked_apps_empty
import unit2026.composeapp.generated.resources.settings_blocked_apps_in_category
import unit2026.composeapp.generated.resources.settings_blocked_apps_search
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
    val installedApps by FocusSessionState.installedApps.collectAsState()

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

        // --- Blocked apps ---
        SettingsCard {
            SectionLabel(stringResource(Res.string.settings_blocked_apps))
            Text(
                text = stringResource(Res.string.settings_blocked_apps_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(spacing.sm))

            if (installedApps.isEmpty()) {
                Text(
                    text = stringResource(Res.string.settings_blocked_apps_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                BlockedAppsList(
                    apps = installedApps,
                    blacklist = blacklist,
                    onToggle = { appId, blocked ->
                        if (blocked) FocusSessionState.addToBlacklist(appId)
                        else FocusSessionState.removeFromBlacklist(appId)
                    },
                )
            }
        }
    }
}

@Composable
private fun BlockedAppsList(
    apps: List<InstalledApp>,
    blacklist: Set<String>,
    onToggle: (appId: String, blocked: Boolean) -> Unit,
) {
    val spacing = OpenJetTracksTheme.spacing
    var searchQuery by remember { mutableStateOf("") }

    // Search field
    OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text(stringResource(Res.string.settings_blocked_apps_search)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(Modifier.height(spacing.sm))

    // Blocked count
    val blockedCount = apps.count { it.appId in blacklist }
    Text(
        text = stringResource(Res.string.settings_blocked_apps_count, blockedCount, apps.size),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(Modifier.height(spacing.sm))

    // Filter by search
    val filtered = if (searchQuery.isBlank()) apps
    else apps.filter { it.appName.contains(searchQuery, ignoreCase = true) }

    // Group by category, ordered by DISPLAY_ORDER
    val grouped = filtered.groupBy { it.category }
    val sortedCategories = AppCategory.DISPLAY_ORDER.filter { it in grouped }

    sortedCategories.forEach { category ->
        val categoryApps = grouped[category] ?: return@forEach
        // Sort: blocked first, then alphabetically
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

    // Category header
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Block-all checkbox
        Checkbox(
            checked = allBlocked,
            onCheckedChange = { onToggleAll(it) },
        )

        // Category name + count
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

    // App rows
    AnimatedVisibility(visible = expanded) {
        Column {
            apps.forEach { app ->
                val isBlocked = app.appId in blacklist
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggle(app.appId, !isBlocked) }
                        .padding(vertical = spacing.xs),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
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
