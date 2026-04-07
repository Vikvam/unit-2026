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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import cz.aaa.unit2026.ui.components.SessionTimeline
import cz.aaa.unit2026.ui.components.TimelineSegment
import cz.aaa.unit2026.ui.theme.OpenJetTracksTheme
import org.jetbrains.compose.resources.stringResource
import unit2026.composeapp.generated.resources.Res
import unit2026.composeapp.generated.resources.report_distracted
import unit2026.composeapp.generated.resources.report_focused
import unit2026.composeapp.generated.resources.report_title
import unit2026.composeapp.generated.resources.report_total

@Composable
fun ReportScreen(
    modifier: Modifier = Modifier,
) {
    val spacing = OpenJetTracksTheme.spacing
    val focus = OpenJetTracksTheme.focus

    // Placeholder data — will come from ViewModel
    val placeholderSegments = listOf(
        TimelineSegment(0f, 0.4f, focused = true),
        TimelineSegment(0.4f, 0.55f, focused = false),
        TimelineSegment(0.55f, 1f, focused = true),
    )

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

        // Timeline card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(spacing.md)) {
                SessionTimeline(segments = placeholderSegments)
            }
        }

        // Stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            StatCard(
                value = "82%",
                label = stringResource(Res.string.report_focused, ""),
                color = focus.active,
                containerColor = focus.activeContainer,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                value = "18%",
                label = stringResource(Res.string.report_distracted, ""),
                color = focus.distracted,
                containerColor = focus.distractedContainer,
                modifier = Modifier.weight(1f),
            )
        }

        // Total session card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(spacing.md)) {
                Text(
                    text = stringResource(Res.string.report_total, "25:00"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
        Column(
            modifier = Modifier.padding(OpenJetTracksTheme.spacing.md),
        ) {
            Text(
                text = value,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = color,
            )
            Spacer(Modifier.height(OpenJetTracksTheme.spacing.xs))
            Text(
                text = label.trimEnd(),
                style = MaterialTheme.typography.bodyMedium,
                color = color.copy(alpha = 0.8f),
            )
        }
    }
}
