package cz.aaa.unit2026.monitoring

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * In-process singleton bridge between FocusAccessibilityService and AndroidForegroundAppMonitor.
 * The service emits here; the monitor exposes it as a Flow.
 */
object ActiveAppBus {
    private val _events = MutableSharedFlow<ActiveApp>(replay = 1)
    val events = _events.asSharedFlow()

    fun emit(app: ActiveApp) {
        _events.tryEmit(app)
    }
}
