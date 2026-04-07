package cz.aaa.unit2026.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cz.aaa.unit2026.ui.theme.OpenJetTracksTheme
import org.jetbrains.compose.resources.stringResource
import unit2026.composeapp.generated.resources.Res
import unit2026.composeapp.generated.resources.blocking_message
import unit2026.composeapp.generated.resources.blocking_title

/**
 * Full-screen overlay shown when the user opens a distracting app/website.
 *
 * @param appName name of the blocked application (shown to the user)
 * @param remainingLabel formatted time remaining in the session (e.g. "23:10")
 */
@Composable
fun BlockingOverlay(
    appName: String,
    remainingLabel: String,
    modifier: Modifier = Modifier,
) {
    val spacing = OpenJetTracksTheme.spacing
    val focus = OpenJetTracksTheme.focus

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(focus.distractedContainer)
            .padding(spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(Res.string.blocking_title),
            style = MaterialTheme.typography.displaySmall,
            color = focus.distracted,
        )

        Spacer(Modifier.height(spacing.md))

        Text(
            text = stringResource(Res.string.blocking_message, appName),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(spacing.lg))

        TimerRing(
            progress = 1f,
            state = TimerState.Running,
            label = remainingLabel,
            size = OpenJetTracksTheme.spacing.xxl * 4,
        )
    }
}
