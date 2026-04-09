package cz.aaa.unit2026.monitoring

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

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
    val status: StateFlow<MonitorStatus>
    fun start()
    fun stop()
}

enum class MonitorStatus {
    /** Not yet started. */
    IDLE,
    /** Running and monitoring the foreground app. */
    RUNNING,
    /** Failed to start — a required system dependency is missing or unavailable. */
    FAILED,
}
