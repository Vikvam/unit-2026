package cz.aaa.unit2026.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import cz.aaa.unit2026.LocalTrackingClient
import cz.aaa.unit2026.ui.components.SessionTimeline
import cz.aaa.unit2026.ui.report.ReportStateHolder
import cz.aaa.unit2026.ui.theme.OpenJetTracksTheme
import org.jetbrains.compose.resources.stringResource
import unit2026.composeapp.generated.resources.Res
import unit2026.composeapp.generated.resources.report_distracted
import unit2026.composeapp.generated.resources.report_focused
import unit2026.composeapp.generated.resources.report_title
import unit2026.composeapp.generated.resources.report_total

@Composable
fun ReportScreen(modifier: Modifier = Modifier) {
    val spacing = OpenJetTracksTheme.spacing
    val client = LocalTrackingClient.current
    val scope = rememberCoroutineScope()
    val holder = remember(client) { ReportStateHolder(client, scope) }
    val uiState by holder.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.lg),
        verticalArrangement = Arrangement.Top,
    ) {
        Text(
            text = stringResource(Res.string.report_title),
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(Modifier.height(spacing.lg))

        if (uiState.hasSession) {
            SessionTimeline(segments = uiState.segments)

            Spacer(Modifier.height(spacing.lg))

            Text(
                text = stringResource(Res.string.report_focused, "${uiState.focusPercent}%"),
                style = MaterialTheme.typography.titleMedium,
                color = OpenJetTracksTheme.focus.active,
            )

            Spacer(Modifier.height(spacing.sm))

            if (uiState.distractedPercent > 0) {
                Text(
                    text = stringResource(Res.string.report_distracted, "${uiState.distractedPercent}%"),
                    style = MaterialTheme.typography.titleMedium,
                    color = OpenJetTracksTheme.focus.distracted,
                )

                Spacer(Modifier.height(spacing.lg))
            } else {
                Spacer(Modifier.height(spacing.lg))
            }

            Text(
                text = stringResource(Res.string.report_total, uiState.durationLabel),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = "No session completed yet. Start a timer to begin tracking.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
