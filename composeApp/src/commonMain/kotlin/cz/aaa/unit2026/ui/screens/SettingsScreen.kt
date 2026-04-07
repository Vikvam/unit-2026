package cz.aaa.unit2026.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cz.aaa.unit2026.ui.theme.OpenJetTracksTheme

/**
 * Settings screen — strict mode toggle, whitelist management, theme, language.
 *
 * TODO: wire to a shared ViewModel once :shared exposes user settings.
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
) {
    val spacing = OpenJetTracksTheme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.lg),
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(Modifier.height(spacing.lg))

        // Strict mode
        SettingsRow(
            label = "Strict mode",
            description = "Block all non-whitelisted apps during sessions",
            checked = false,
            onCheckedChange = { /* TODO */ },
        )

        Spacer(Modifier.height(spacing.md))

        // Dark theme
        SettingsRow(
            label = "Dark theme",
            description = "Follow system setting",
            checked = false,
            onCheckedChange = { /* TODO */ },
        )
    }
}

@Composable
private fun SettingsRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
