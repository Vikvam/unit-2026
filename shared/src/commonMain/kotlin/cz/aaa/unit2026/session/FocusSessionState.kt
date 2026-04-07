package cz.aaa.unit2026.session

import cz.aaa.unit2026.blocking.BlockingEnforcer
import cz.aaa.unit2026.monitoring.AppCategory
import cz.aaa.unit2026.blocking.DEFAULT_BLACKLIST
import cz.aaa.unit2026.blocking.InstalledApp
import cz.aaa.unit2026.monitoring.ActiveApp
import cz.aaa.unit2026.monitoring.ForegroundAppMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
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
 * - installedApps: populated by platform (Android: PackageManager, Desktop: empty)
 */
object FocusSessionState {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var monitor: ForegroundAppMonitor? = null
    private var enforcer: BlockingEnforcer? = null
    private var monitorJob: Job? = null
    private var timerJob: Job? = null

    // --- Session state ---
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(0L)
    val remainingSeconds: StateFlow<Long> = _remainingSeconds.asStateFlow()

    private val _durationSeconds = MutableStateFlow(25L * 60)

    // --- Blocking ---
    private val _isStrictMode = MutableStateFlow(true)
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
        // Auto-block apps in default-blocked categories (e.g. Games) that aren't already known
        val autoBlock = apps
            .filter { it.category in AppCategory.DEFAULT_BLOCKED && it.appId !in _blacklist.value }
            .map { it.appId }
            .toSet()
        if (autoBlock.isNotEmpty()) {
            _blacklist.value = _blacklist.value + autoBlock
        }
    }

    fun setDurationMinutes(minutes: Int) {
        _durationSeconds.value = minutes.toLong() * 60
    }

    // --- Timer controls ---

    fun startSession() {
        if (_isRunning.value) return
        _isRunning.value = true
        _isPaused.value = false
        _remainingSeconds.value = _durationSeconds.value

        startTimer()
        startMonitoring()
    }

    fun pauseSession() {
        if (!_isRunning.value || _isPaused.value) return
        _isPaused.value = true
        timerJob?.cancel()
        timerJob = null
        enforcer?.unblock()
        _blockedApp.value = null
    }

    fun resumeSession() {
        if (!_isRunning.value || !_isPaused.value) return
        _isPaused.value = false
        startTimer()
    }

    fun stopSession() {
        _isRunning.value = false
        _isPaused.value = false
        _remainingSeconds.value = 0L
        timerJob?.cancel()
        timerJob = null
        monitorJob?.cancel()
        monitorJob = null
        monitor?.stop()
        enforcer?.unblock()
        _blockedApp.value = null
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (_remainingSeconds.value > 0) {
                delay(1000L)
                if (!_isPaused.value) {
                    _remainingSeconds.value = (_remainingSeconds.value - 1).coerceAtLeast(0)
                }
            }
            // Session finished naturally
            stopSession()
        }
    }

    private fun startMonitoring() {
        monitor?.start()
        monitorJob = scope.launch {
            monitor?.activeApp?.collect { app ->
                if (_isStrictMode.value && !_isPaused.value && app.appId !in SELF_APP_IDS) {
                    if (app.appId in _blacklist.value) {
                        enforcer?.block(app)
                        _blockedApp.value = app
                    }
                }
            }
        }
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

    fun addAllToBlacklist(appIds: Set<String>) {
        _blacklist.value = _blacklist.value + appIds
    }

    fun removeAllFromBlacklist(appIds: Set<String>) {
        _blacklist.value = _blacklist.value - appIds
    }

    private val SELF_APP_IDS = setOf(
        "cz-aaa-unit2026-MainKt",
        "java-lang-Thread",
        "cz.aaa.unit2026",
    )
}
