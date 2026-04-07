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

/**
 * Post-session report — shows whether focus was respected, with a timeline
 * of focused vs. distracted periods.
 *
 * TODO: wire to a shared ViewModel once :shared exposes completed session data.
 */
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
            text = "Session Report",
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(Modifier.height(spacing.lg))

        SessionTimeline(segments = placeholderSegments)

        Spacer(Modifier.height(spacing.lg))

        Text(
            text = "Focused: 82%",
            style = MaterialTheme.typography.titleMedium,
            color = OpenJetTracksTheme.focus.active,
        )

        Spacer(Modifier.height(spacing.sm))

        Text(
            text = "Distracted: 18%",
            style = MaterialTheme.typography.titleMedium,
            color = OpenJetTracksTheme.focus.distracted,
        )

        Spacer(Modifier.height(spacing.lg))

        Text(
            text = "25:00 total session",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
