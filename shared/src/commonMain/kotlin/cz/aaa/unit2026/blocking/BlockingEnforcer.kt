package cz.aaa.unit2026.blocking

import cz.aaa.unit2026.monitoring.ActiveApp
import kotlinx.coroutines.flow.StateFlow

/**
 * Blocks the user from accessing a distraction app by showing a full-screen overlay.
 *
 * Implementations:
 * - Desktop/JVM: always-on-top undecorated Compose window
 * - Android: TYPE_APPLICATION_OVERLAY via SYSTEM_ALERT_WINDOW
 */
interface BlockingEnforcer {
    val blockedApp: StateFlow<ActiveApp?>
    fun block(app: ActiveApp)
    fun unblock()
}
