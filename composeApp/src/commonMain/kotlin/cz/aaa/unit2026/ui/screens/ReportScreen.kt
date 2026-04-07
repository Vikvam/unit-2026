package cz.aaa.unit2026.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.aaa.unit2026.session.AppUsageStat
import cz.aaa.unit2026.session.DistractionAttempt
import cz.aaa.unit2026.session.FocusSessionRecord
import cz.aaa.unit2026.session.FocusSessionState
import cz.aaa.unit2026.ui.components.SessionTimeline
import cz.aaa.unit2026.ui.components.TimelineSegment
import cz.aaa.unit2026.ui.components.TimerRing
import cz.aaa.unit2026.ui.components.TimerState
import cz.aaa.unit2026.ui.theme.OpenJetTracksTheme
import org.jetbrains.compose.resources.stringResource
import unit2026.composeapp.generated.resources.Res
import unit2026.composeapp.generated.resources.report_title

@Composable
fun ReportScreen(modifier: Modifier = Modifier) {
    val spacing = OpenJetTracksTheme.spacing
    val sessions by FocusSessionState.sessionHistory.collectAsState()
    val appUsage by FocusSessionState.appUsage.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        item {
            Text(stringResource(Res.string.report_title), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        }

        if (sessions.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(top = spacing.xxl), contentAlignment = Alignment.Center) {
                    Text(
                        "No sessions yet. Start a focus session to see your stats.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            return@LazyColumn
        }

        item { OverviewRow(sessions) }

        item {
            SectionHeader("Last session")
            SessionCard(sessions.last())
        }

        if (sessions.size > 1) {
            item {
                SectionHeader("Session durations")
                SessionBarChart(sessions)
            }
        }

        val allDistractions = sessions.flatMap { it.distractions }
        if (allDistractions.isNotEmpty()) {
            val ranked = allDistractions
                .groupBy { it.matchedRule ?: it.appName }
                .entries.sortedByDescending { it.value.size }.take(5)
            item { SectionHeader("Top distractions") }
            itemsIndexed(ranked) { i, (name, attempts) -> DistractionRow(i + 1, name, attempts) }
        }

        // App usage
        val usageList = appUsage.values
            .filter { it.totalMs > 5_000 } // skip blips under 5s
            .sortedByDescending { it.totalMs }
            .take(8)
        if (usageList.isNotEmpty()) {
            item {
                SectionHeader("App usage")
                AppUsageList(usageList)
            }
        }

        item {
            SectionHeader("Records")
            PersonalRecords(sessions)
        }

        item { Spacer(Modifier.height(spacing.lg)) }
    }
}

@Composable
private fun OverviewRow(sessions: List<FocusSessionRecord>) {
    val spacing = OpenJetTracksTheme.spacing
    val totalMinutes = sessions.sumOf { it.durationMinutes }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
        StatCard("%dh %02dm".format(totalMinutes / 60, totalMinutes % 60), "Focus time", Modifier.weight(1f))
        StatCard("${sessions.count { it.completed }} / ${sessions.size}", "Done", Modifier.weight(1f))
        StatCard("${sessions.map { it.focusScore }.average().toInt()}%", "Avg score", Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    val spacing = OpenJetTracksTheme.spacing
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SessionCard(session: FocusSessionRecord) {
    val spacing = OpenJetTracksTheme.spacing
    val focus = OpenJetTracksTheme.focus
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(spacing.md).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TimerRing(
                progress = session.focusScore / 100f,
                state = if (session.completed) TimerState.Running else TimerState.Paused,
                label = "${session.focusScore}%",
                size = 96.dp,
                labelSize = 18.sp,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    Text(
                        if (session.completed) "✓ Completed" else "Stopped early",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (session.completed) focus.active else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text("${session.durationMinutes}m", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                SessionTimeline(segments = session.toTimelineSegments())
                if (session.distractions.isEmpty()) {
                    Text("No distractions", style = MaterialTheme.typography.bodySmall, color = focus.active)
                } else {
                    Text("${session.distractions.size} distraction attempt${if (session.distractions.size == 1) "" else "s"}", style = MaterialTheme.typography.bodySmall, color = focus.distracted)
                }
            }
        }
    }
}

@Composable
private fun SessionBarChart(sessions: List<FocusSessionRecord>) {
    val focus = OpenJetTracksTheme.focus
    val maxMinutes = sessions.maxOf { it.durationMinutes }.coerceAtLeast(1)
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth().height(80.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
            sessions.forEach { session ->
                val fraction = session.durationMinutes.toFloat() / maxMinutes
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .fillMaxHeight(fraction.coerceAtLeast(0.05f))
                                .background(
                                    color = if (session.completed) focus.active else focus.distracted,
                                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                                )
                        )
                    }
                    Text("${session.durationMinutes}m", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun DistractionRow(rank: Int, name: String, attempts: List<DistractionAttempt>) {
    val spacing = OpenJetTracksTheme.spacing
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = spacing.xs), horizontalArrangement = Arrangement.spacedBy(spacing.md), verticalAlignment = Alignment.CenterVertically) {
        Text("#$rank", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(24.dp))
        Text(name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${attempts.size}×", style = MaterialTheme.typography.labelMedium, color = OpenJetTracksTheme.focus.distracted, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PersonalRecords(sessions: List<FocusSessionRecord>) {
    val spacing = OpenJetTracksTheme.spacing
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            sessions.maxByOrNull { it.durationMinutes }?.let { RecordRow("Longest session", "${it.durationMinutes} min") }
            sessions.maxByOrNull { it.focusScore }?.let { RecordRow("Best focus score", "${it.focusScore}%") }
            RecordRow("Distractions resisted", "${sessions.sumOf { it.distractions.size }}")
            RecordRow("Sessions completed", "${sessions.count { it.completed }} / ${sessions.size}")
        }
    }
}

@Composable
private fun RecordRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AppUsageList(stats: List<AppUsageStat>) {
    val spacing = OpenJetTracksTheme.spacing
    val focus = OpenJetTracksTheme.focus
    val maxMs = stats.maxOf { it.totalMs }.coerceAtLeast(1L)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            stats.forEach { stat ->
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stat.appName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            formatDuration(stat.totalMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    // Split bar: green = focus time, gray = off-focus time
                    val totalFraction = stat.totalMs.toFloat() / maxMs
                    val focusFraction = if (stat.totalMs > 0) stat.focusMs.toFloat() / stat.totalMs else 0f
                    Box(
                        modifier = Modifier.fillMaxWidth().height(6.dp).background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(3.dp),
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(totalFraction)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(3.dp))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(totalFraction * focusFraction)
                                .fillMaxHeight()
                                .background(focus.active, RoundedCornerShape(3.dp))
                        )
                        if (totalFraction * (1f - focusFraction) > 0.01f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(totalFraction)
                                    .fillMaxHeight()
                                    .padding(start = (totalFraction * focusFraction * 300).dp.coerceAtMost(200.dp))
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(3.dp))
                            )
                        }
                    }
                    if (stat.focusMs > 0 && stat.offFocusMs > 0) {
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                            Text(
                                "Focus: ${formatDuration(stat.focusMs)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = focus.active,
                            )
                            Text(
                                "Other: ${formatDuration(stat.offFocusMs)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSecs = ms / 1000
    val h = totalSecs / 3600
    val m = (totalSecs % 3600) / 60
    val s = totalSecs % 60
    return when {
        h > 0 -> "${h}h ${m}m"
        m > 0 -> "${m}m ${s}s"
        else  -> "${s}s"
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = OpenJetTracksTheme.focus.active, modifier = Modifier.padding(bottom = 4.dp))
}

private fun FocusSessionRecord.toTimelineSegments(): List<TimelineSegment> {
    if (distractions.isEmpty()) return listOf(TimelineSegment(0f, 1f, focused = true))
    val duration = (endTimeMs - startTimeMs).toFloat().coerceAtLeast(1f)
    val segments = mutableListOf<TimelineSegment>()
    var cursor = 0f
    distractions.sortedBy { it.timestampMs }.forEach { d ->
        val pos = ((d.timestampMs - startTimeMs) / duration).coerceIn(0f, 1f)
        if (pos > cursor) segments.add(TimelineSegment(cursor, pos, focused = true))
        val end = (pos + 0.03f).coerceAtMost(1f)
        segments.add(TimelineSegment(pos, end, focused = false))
        cursor = end
    }
    if (cursor < 1f) segments.add(TimelineSegment(cursor, 1f, focused = true))
    return segments
}
