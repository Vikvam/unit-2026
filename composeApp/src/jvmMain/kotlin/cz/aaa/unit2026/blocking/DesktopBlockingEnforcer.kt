package cz.aaa.unit2026.blocking

import cz.aaa.unit2026.monitoring.ActiveApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DesktopBlockingEnforcer : BlockingEnforcer {

    private val _blockedApp = MutableStateFlow<ActiveApp?>(null)
    override val blockedApp: StateFlow<ActiveApp?> = _blockedApp

    override fun block(app: ActiveApp) {
        _blockedApp.value = app
    }

    override fun unblock() {
        _blockedApp.value = null
    }
}
