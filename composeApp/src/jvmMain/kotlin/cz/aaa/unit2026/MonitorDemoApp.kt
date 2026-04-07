package cz.aaa.unit2026

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import cz.aaa.unit2026.monitoring.KdeWaylandAppMonitor

@Composable
fun MonitorDemoApp() {
    val scope = rememberCoroutineScope()
    val monitor = remember { KdeWaylandAppMonitor(scope) }
    MonitorDemoScreen(monitor)
}
