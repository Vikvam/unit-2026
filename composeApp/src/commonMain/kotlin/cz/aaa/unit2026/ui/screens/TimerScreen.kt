package cz.aaa.unit2026.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import cz.aaa.unit2026.session.FocusSessionState
import cz.aaa.unit2026.ui.components.TimerRing
import cz.aaa.unit2026.ui.components.TimerState
import cz.aaa.unit2026.ui.theme.OpenJetTracksTheme
import cz.aaa.unit2026.ui.theme.SessionSettings
import org.jetbrains.compose.resources.stringResource
import unit2026.composeapp.generated.resources.Res
import unit2026.composeapp.generated.resources.settings_duration
import unit2026.composeapp.generated.resources.settings_duration_minutes
import unit2026.composeapp.generated.resources.settings_strict_mode
import unit2026.composeapp.generated.resources.settings_strict_mode_desc
import unit2026.composeapp.generated.resources.timer_start
import unit2026.composeapp.generated.resources.timer_stop
import unit2026.composeapp.generated.resources.timer_tap_hint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    modifier: Modifier = Modifier,
) {
    val spacing = OpenJetTracksTheme.spacing

    val durationMinutes by SessionSettings.durationMinutes.collectAsState()
    val isRunning by FocusSessionState.isRunning.collectAsState()
    val progress = 0f
    val timerState = if (isRunning) TimerState.Running else TimerState.Idle
    val label = "%d:%02d".format(durationMinutes, 0)

    var showSessionParams by remember { mutableStateOf(false) }

    // Press-down scale effect on the ring
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "ringScale",
    )

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        TimerRing(
            progress = progress,
            state = timerState,
            label = label,
            subtitle = stringResource(Res.string.timer_tap_hint),
            modifier = Modifier
                .scale(scale)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            pressed = true
                            tryAwaitRelease()
                            pressed = false
                        },
                        onTap = { showSessionParams = true },
                    )
                },
        )

        Spacer(Modifier.height(spacing.xxl))

        Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
            Button(
                onClick = { FocusSessionState.startSession() },
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.height(48.dp).width(120.dp),
            ) {
                Text(stringResource(Res.string.timer_start))
            }

            OutlinedButton(
                onClick = { FocusSessionState.stopSession() },
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                modifier = Modifier.height(48.dp).width(120.dp),
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
    val isStrictMode by FocusSessionState.isStrictMode.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg)
            .padding(bottom = spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        // Duration
        Column {
            Text(
                text = stringResource(Res.string.settings_duration),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
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
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = spacing.sm),
                )
            }
        }

        // Strict mode
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.settings_strict_mode),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(Res.string.settings_strict_mode_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = isStrictMode,
                onCheckedChange = { FocusSessionState.setStrictMode(it) },
            )
        }

        // TODO: Whitelist — apps/websites allowed during focus sessions
        // TODO: Blocklist — apps/websites always blocked during focus sessions
    }
}
