package cz.aaa.unit2026.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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
    val focus = OpenJetTracksTheme.focus

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Text(
            text = stringResource(Res.string.report_title),
            style = MaterialTheme.typography.headlineMedium,
        )

        if (uiState.hasSession) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(spacing.md)) {
                    SessionTimeline(segments = uiState.segments)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                StatCard(
                    value = "${uiState.focusPercent}%",
                    label = stringResource(Res.string.report_focused, "").trimEnd(),
                    color = focus.active,
                    containerColor = focus.activeContainer,
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    value = "${uiState.distractedPercent}%",
                    label = stringResource(Res.string.report_distracted, "").trimEnd(),
                    color = focus.distracted,
                    containerColor = focus.distractedContainer,
                    modifier = Modifier.weight(1f),
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(spacing.md)) {
                    Text(
                        text = stringResource(Res.string.report_total, uiState.durationLabel),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            Spacer(Modifier.height(spacing.xxl))
            Text(
                text = "No session completed yet. Start a timer to begin tracking.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    color: Color,
    containerColor: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(OpenJetTracksTheme.spacing.md)) {
            Text(
                text = value,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = color,
            )
            Spacer(Modifier.height(OpenJetTracksTheme.spacing.xs))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = color.copy(alpha = 0.8f),
            )
        }
    }
}
