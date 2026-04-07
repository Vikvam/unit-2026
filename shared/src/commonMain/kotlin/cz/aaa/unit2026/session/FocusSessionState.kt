package cz.aaa.unit2026.session

import cz.aaa.unit2026.blocking.BlockRule
import cz.aaa.unit2026.blocking.BlockingEnforcer
import cz.aaa.unit2026.blocking.DEFAULT_BLACKLIST
import cz.aaa.unit2026.blocking.DEFAULT_BLOCK_RULES
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
 * The monitor runs passively from init() for debug screen + seenApps tracking.
 * Blocking only activates during a running, non-paused session with strict mode on.
 */
object FocusSessionState {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var enforcer: BlockingEnforcer? = null
    private var timerJob: Job? = null

    // --- Session history ---
    private val _sessionHistory = MutableStateFlow<List<FocusSessionRecord>>(emptyList())
    val sessionHistory: StateFlow<List<FocusSessionRecord>> = _sessionHistory.asStateFlow()

    private var sessionStartMs = 0L
    private var plannedSeconds = 0L
    private val currentDistractions = mutableListOf<DistractionAttempt>()

    // --- Session state ---
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(0L)
    val remainingSeconds: StateFlow<Long> = _remainingSeconds.asStateFlow()

    private val _durationSeconds = MutableStateFlow(25L * 60)

    // --- Passive monitor output ---
    private val _currentApp = MutableStateFlow<ActiveApp?>(null)
    val currentApp: StateFlow<ActiveApp?> = _currentApp.asStateFlow()

    private val _seenApps = MutableStateFlow<Map<String, ActiveApp>>(emptyMap())
    val seenApps: StateFlow<Map<String, ActiveApp>> = _seenApps.asStateFlow()

    // --- Blocking ---
    private val _isStrictMode = MutableStateFlow(false)
    val isStrictMode: StateFlow<Boolean> = _isStrictMode.asStateFlow()

    private val _blacklist = MutableStateFlow(DEFAULT_BLACKLIST.toMutableSet() as Set<String>)
    val blacklist: StateFlow<Set<String>> = _blacklist.asStateFlow()

    private val _blockRules = MutableStateFlow(DEFAULT_BLOCK_RULES)
    val blockRules: StateFlow<List<BlockRule>> = _blockRules.asStateFlow()

    private val _blockedApp = MutableStateFlow<ActiveApp?>(null)
    val blockedApp: StateFlow<ActiveApp?> = _blockedApp.asStateFlow()

    // --- App list (populated by platform) ---
    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val installedApps: StateFlow<List<InstalledApp>> = _installedApps.asStateFlow()

    // --- Platform setup ---

    fun init(monitor: ForegroundAppMonitor, enforcer: BlockingEnforcer) {
        this.enforcer = enforcer
        monitor.start()
        scope.launch {
            monitor.activeApp.collect { app ->
                _currentApp.value = app
                if (app.appId !in SELF_APP_IDS) {
                    _seenApps.value = _seenApps.value + (app.appId to app)
                }
                if (_isRunning.value && !_isPaused.value && _isStrictMode.value && app.appId !in SELF_APP_IDS) {
                    if (shouldBlock(app)) {
                        val alreadyBlocked = _blockedApp.value?.appId == app.appId
                        if (!alreadyBlocked) {
                            val matchedRule = _blockRules.value.filter { it.enabled }
                                .firstOrNull { rule ->
                                    runCatching {
                                        Regex(rule.pattern, RegexOption.IGNORE_CASE)
                                            .containsMatchIn("${app.appId} ${app.windowTitle ?: ""}")
                                    }.getOrDefault(false)
                                }
                            currentDistractions.add(
                                DistractionAttempt(
                                    timestampMs = System.currentTimeMillis(),
                                    appId = app.appId,
                                    appName = app.appName,
                                    matchedRule = matchedRule?.label,
                                )
                            )
                        }
                        enforcer.block(app)
                        _blockedApp.value = app
                    } else {
                        enforcer.unblock()
                        _blockedApp.value = null
                    }
                }
            }
        }
    }

    fun setInstalledApps(apps: List<InstalledApp>) {
        _installedApps.value = apps
    }

    fun setDurationMinutes(minutes: Int) {
        _durationSeconds.value = minutes.toLong() * 60
    }

    // --- Session controls ---

    fun startSession() {
        if (_isRunning.value) return
        _isRunning.value = true
        _isPaused.value = false
        _remainingSeconds.value = _durationSeconds.value
        sessionStartMs = System.currentTimeMillis()
        plannedSeconds = _durationSeconds.value
        currentDistractions.clear()
        startTimer()
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
        if (_isRunning.value && sessionStartMs > 0L) {
            saveSession(completed = _remainingSeconds.value == 0L)
        }
        _isRunning.value = false
        _isPaused.value = false
        _remainingSeconds.value = 0L
        timerJob?.cancel()
        timerJob = null
        enforcer?.unblock()
        _blockedApp.value = null
    }

    private fun saveSession(completed: Boolean) {
        val record = FocusSessionRecord(
            id = sessionStartMs,
            startTimeMs = sessionStartMs,
            endTimeMs = System.currentTimeMillis(),
            plannedSeconds = plannedSeconds,
            completed = completed,
            distractions = currentDistractions.toList(),
        )
        _sessionHistory.value = _sessionHistory.value + record
        currentDistractions.clear()
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
            stopSession()
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

    fun addBlockRule(rule: BlockRule) {
        _blockRules.value = _blockRules.value + rule
    }

    fun removeBlockRule(rule: BlockRule) {
        _blockRules.value = _blockRules.value - rule
    }

    fun toggleBlockRule(rule: BlockRule) {
        _blockRules.value = _blockRules.value.map {
            if (it == rule) it.copy(enabled = !it.enabled) else it
        }
    }

    private fun shouldBlock(app: ActiveApp): Boolean {
        if (app.appId in _blacklist.value) return true
        val target = "${app.appId} ${app.windowTitle ?: ""}"
        return _blockRules.value
            .filter { it.enabled }
            .any { rule ->
                runCatching { Regex(rule.pattern, RegexOption.IGNORE_CASE).containsMatchIn(target) }
                    .getOrDefault(false)
            }
    }

    private val SELF_APP_IDS = setOf(
        "cz-aaa-unit2026-MainKt",
        "java-lang-Thread",
        "cz.aaa.unit2026",
    )
}
