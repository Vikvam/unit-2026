package cz.aaa.unit2026.ui.screens

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

        // Block rules (regex — desktop browser titles)
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
        } // end SettingsCard (block rules)

        // Blocked apps — Android: installed apps, Desktop: seen apps
        SettingsCard {
            SectionLabel("Blocked apps")
        Text(
            "Toggle to block entire apps during focus sessions.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(spacing.sm))

        val appList = if (installedApps.isNotEmpty()) {
            installedApps.map { it.appId to it.appName }
        } else {
            seenApps.values.sortedBy { it.appId }.map { it.appId to it.appName }
        }

        if (appList.isEmpty()) {
            Text(
                "No apps seen yet. Apps will appear here as you use your device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            appList.forEach { (appId, appName) ->
                val isBlocked = appId in blacklist
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(appName, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            appId,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Checkbox(
                        checked = isBlocked,
                        onCheckedChange = { checked ->
                            if (checked) FocusSessionState.addToBlacklist(appId)
                            else FocusSessionState.removeFromBlacklist(appId)
                        },
                    )
                }
            }
        }
        } // end SettingsCard (blocked apps)
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
