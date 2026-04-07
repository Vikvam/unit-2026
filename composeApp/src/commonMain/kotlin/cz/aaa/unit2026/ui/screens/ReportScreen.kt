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
import androidx.compose.ui.Modifier
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
        verticalArrangement = Arrangement.Top,
    ) {
        Text(
            text = stringResource(Res.string.report_title),
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(Modifier.height(spacing.lg))

        SessionTimeline(segments = placeholderSegments)

        Spacer(Modifier.height(spacing.lg))

        Text(
            text = stringResource(Res.string.report_focused, "82%"),
            style = MaterialTheme.typography.titleMedium,
            color = OpenJetTracksTheme.focus.active,
        )

        Spacer(Modifier.height(spacing.sm))

        Text(
            text = stringResource(Res.string.report_distracted, "18%"),
            style = MaterialTheme.typography.titleMedium,
            color = OpenJetTracksTheme.focus.distracted,
        )

        Spacer(Modifier.height(spacing.lg))

        Text(
            text = stringResource(Res.string.report_total, "25:00"),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
