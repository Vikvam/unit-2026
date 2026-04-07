package cz.aaa.unit2026.session

import cz.aaa.unit2026.blocking.BlockingEnforcer
import cz.aaa.unit2026.blocking.DEFAULT_BLACKLIST
import cz.aaa.unit2026.blocking.InstalledApp
import cz.aaa.unit2026.monitoring.ActiveApp
import cz.aaa.unit2026.monitoring.ForegroundAppMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Central state holder connecting the UI to the monitoring and blocking layers.
 *
 * Provided at startup by each platform:
 * - monitor: ForegroundAppMonitor (KDE/Windows on desktop, AccessibilityService on Android)
 * - enforcer: BlockingEnforcer (overlay on desktop, home action on Android)
 * - installedApps: populated by platform (Android: PackageManager, Desktop: empty — see placeholder in SettingsScreen)
 */
object FocusSessionState {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var monitor: ForegroundAppMonitor? = null
    private var enforcer: BlockingEnforcer? = null
    private var monitorJob: Job? = null

    // --- Timer ---
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    // --- Blocking ---
    private val _isStrictMode = MutableStateFlow(false)
    val isStrictMode: StateFlow<Boolean> = _isStrictMode.asStateFlow()

    private val _blacklist = MutableStateFlow(DEFAULT_BLACKLIST.toMutableSet() as Set<String>)
    val blacklist: StateFlow<Set<String>> = _blacklist.asStateFlow()

    private val _blockedApp = MutableStateFlow<ActiveApp?>(null)
    val blockedApp: StateFlow<ActiveApp?> = _blockedApp.asStateFlow()

    // --- App list (populated by platform) ---
    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val installedApps: StateFlow<List<InstalledApp>> = _installedApps.asStateFlow()

    // --- Platform setup ---

    fun init(monitor: ForegroundAppMonitor, enforcer: BlockingEnforcer) {
        this.monitor = monitor
        this.enforcer = enforcer
    }

    fun setInstalledApps(apps: List<InstalledApp>) {
        _installedApps.value = apps
    }

    // --- Timer controls ---

    fun startSession() {
        if (_isRunning.value) return
        _isRunning.value = true
        monitor?.start()
        monitorJob = scope.launch {
            monitor?.activeApp?.collect { app ->
                if (_isStrictMode.value && app.appId !in SELF_APP_IDS) {
                    if (app.appId in _blacklist.value) {
                        enforcer?.block(app)
                        _blockedApp.value = app
                    }
                }
            }
        }
    }

    fun stopSession() {
        _isRunning.value = false
        monitorJob?.cancel()
        monitorJob = null
        monitor?.stop()
        enforcer?.unblock()
        _blockedApp.value = null
    }

    // --- Settings ---

    fun setStrictMode(enabled: Boolean) {
        _isStrictMode.value = enabled
        if (!enabled) {
            enforcer?.unblock()
            _blockedApp.value = null
        }
    }

    fun addToBlacklist(appId: String) {
        _blacklist.value = _blacklist.value + appId
    }

    fun removeFromBlacklist(appId: String) {
        _blacklist.value = _blacklist.value - appId
    }

    private val SELF_APP_IDS = setOf(
        "cz-aaa-unit2026-MainKt",
        "java-lang-Thread",
        "cz.aaa.unit2026",
    )
}
