package cz.aaa.unit2026.monitoring

import kotlinx.coroutines.flow.Flow

/**
 * Observes which app the user is currently focused on.
 *
 * Implementations are platform-specific:
 * - Desktop/JVM: KWin scripting (KDE Wayland), JNA Win32, NSWorkspace (macOS)
 * - Android: UsageStatsManager / AccessibilityService
 *
 * The flow emits only on actual changes (distinctUntilChanged).
 * Call [start] before collecting and [stop] when done.
 */
interface ForegroundAppMonitor {
    val activeApp: Flow<ActiveApp>
    fun start()
    fun stop()
}
