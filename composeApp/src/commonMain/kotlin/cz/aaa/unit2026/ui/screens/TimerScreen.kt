package cz.aaa.unit2026.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cz.aaa.unit2026.ui.components.TimerRing
import cz.aaa.unit2026.ui.components.TimerState
import cz.aaa.unit2026.ui.theme.OpenJetTracksTheme
import cz.aaa.unit2026.ui.theme.SessionSettings
import org.jetbrains.compose.resources.stringResource
import unit2026.composeapp.generated.resources.Res
import unit2026.composeapp.generated.resources.settings_duration
import unit2026.composeapp.generated.resources.settings_duration_minutes
import unit2026.composeapp.generated.resources.timer_start
import unit2026.composeapp.generated.resources.timer_stop

/**
 * Main screen — shows the session timer and start/pause/stop controls.
 *
 * TODO: wire to a shared ViewModel once :shared exposes FocusSession state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    modifier: Modifier = Modifier,
) {
    val spacing = OpenJetTracksTheme.spacing

    // Placeholder state — will be replaced by ViewModel collection
    val durationMinutes by SessionSettings.durationMinutes.collectAsState()
    val progress = 0f
    val timerState = TimerState.Idle
    val label = "%d:%02d".format(durationMinutes, 0)

    var showSessionParams by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        TimerRing(
            progress = progress,
            state = timerState,
            label = label,
            modifier = Modifier.clickable { showSessionParams = true },
        )

        Spacer(Modifier.height(spacing.xl))

        Row {
            Button(onClick = { /* TODO: start / pause */ }) {
                Text(stringResource(Res.string.timer_start))
            }

            Spacer(Modifier.width(spacing.md))

            OutlinedButton(
                onClick = { /* TODO: stop session */ },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(stringResource(Res.string.timer_stop))
            }
        }
    }

    if (showSessionParams) {
        ModalBottomSheet(
            onDismissRequest = { showSessionParams = false },
            sheetState = rememberModalBottomSheetState(),
        ) {
            SessionParamsSheet(durationMinutes = durationMinutes)
        }
    }
}

@Composable
private fun SessionParamsSheet(durationMinutes: Int) {
    val spacing = OpenJetTracksTheme.spacing

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg)
            .padding(bottom = spacing.xl),
    ) {
        Text(
            text = stringResource(Res.string.settings_duration),
            style = MaterialTheme.typography.titleMedium,
        )

        Spacer(Modifier.height(spacing.sm))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Slider(
                value = durationMinutes.toFloat(),
                onValueChange = { SessionSettings.setDuration(it.toInt()) },
                valueRange = 1f..180f,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(Res.string.settings_duration_minutes, durationMinutes),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = spacing.sm),
            )
        }

        // TODO: Whitelist — apps/websites allowed during focus sessions
        // Spacer(Modifier.height(spacing.lg))
        // Text("Whitelist", style = MaterialTheme.typography.titleMedium)
        // WhitelistEditor(...)

        // TODO: Blocklist — apps/websites always blocked during focus sessions
        // Spacer(Modifier.height(spacing.lg))
        // Text("Blocklist", style = MaterialTheme.typography.titleMedium)
        // BlocklistEditor(...)
    }
}
