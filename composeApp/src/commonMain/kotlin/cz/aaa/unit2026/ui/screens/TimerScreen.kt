package cz.aaa.unit2026.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cz.aaa.unit2026.ui.components.TimerRing
import cz.aaa.unit2026.ui.components.TimerState
import cz.aaa.unit2026.ui.theme.OpenJetTracksTheme
import org.jetbrains.compose.resources.stringResource
import unit2026.composeapp.generated.resources.Res
import unit2026.composeapp.generated.resources.timer_start
import unit2026.composeapp.generated.resources.timer_stop

/**
 * Main screen — shows the session timer and start/pause/stop controls.
 *
 * TODO: wire to a shared ViewModel once :shared exposes FocusSession state.
 */
@Composable
fun TimerScreen(
    modifier: Modifier = Modifier,
) {
    val spacing = OpenJetTracksTheme.spacing

    // Placeholder state — will be replaced by ViewModel collection
    val progress = 0f
    val timerState = TimerState.Idle
    val label = "25:00"

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        TimerRing(
            progress = progress,
            state = timerState,
            label = label,
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
}
