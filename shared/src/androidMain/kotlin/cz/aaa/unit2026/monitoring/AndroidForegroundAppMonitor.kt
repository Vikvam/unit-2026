package cz.aaa.unit2026.monitoring

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

class AndroidForegroundAppMonitor : ForegroundAppMonitor {

    override val activeApp: Flow<ActiveApp> = ActiveAppBus.events
        .distinctUntilChanged()

    // Lifecycle is owned by FocusAccessibilityService — nothing to start/stop here.
    override fun start() = Unit
    override fun stop() = Unit
}
