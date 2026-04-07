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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cz.aaa.unit2026.LocalTrackingClient
import cz.aaa.unit2026.ui.components.TimerRing
import cz.aaa.unit2026.ui.components.TimerState
import cz.aaa.unit2026.ui.theme.OpenJetTracksTheme
import cz.aaa.unit2026.ui.timer.TimerStateHolder
import org.jetbrains.compose.resources.stringResource
import unit2026.composeapp.generated.resources.Res
import unit2026.composeapp.generated.resources.timer_start
import unit2026.composeapp.generated.resources.timer_stop

@Composable
fun TimerScreen(modifier: Modifier = Modifier) {
    val spacing = OpenJetTracksTheme.spacing
    val client = LocalTrackingClient.current
    val scope = rememberCoroutineScope()
    val holder = remember(client) { TimerStateHolder(client, scope) }
    val uiState by holder.uiState.collectAsState()

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        TimerRing(
            progress = uiState.progress,
            state = uiState.timerState,
            label = uiState.label,
        )

        Spacer(Modifier.height(spacing.xl))

        Row {
            Button(
                onClick = holder::onStart,
                enabled = uiState.timerState == TimerState.Idle || uiState.timerState == TimerState.Finished,
            ) {
                Text(stringResource(Res.string.timer_start))
            }

            Spacer(Modifier.width(spacing.md))

            OutlinedButton(
                onClick = holder::onStop,
                enabled = uiState.timerState == TimerState.Running,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(stringResource(Res.string.timer_stop))
            }
        }
    }
}
