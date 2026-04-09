package cz.aaa.unit2026.monitoring

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class AndroidForegroundAppMonitor : ForegroundAppMonitor {

    override val activeApp: Flow<ActiveApp> = ActiveAppBus.events
        .distinctUntilChanged()

    // Lifecycle is owned by FocusAccessibilityService — always considered running.
    override val status: StateFlow<MonitorStatus> = MutableStateFlow(MonitorStatus.RUNNING)

    override fun start() = Unit
    override fun stop() = Unit
}
