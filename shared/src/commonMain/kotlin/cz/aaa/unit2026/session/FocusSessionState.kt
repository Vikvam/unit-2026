package cz.aaa.unit2026.session

import cz.aaa.unit2026.AppStorage
import cz.aaa.unit2026.NoOpAppStorage
import cz.aaa.unit2026.blocking.BlockRule
import cz.aaa.unit2026.blocking.BlockingEnforcer
import cz.aaa.unit2026.monitoring.AppCategory
import cz.aaa.unit2026.blocking.DEFAULT_BLACKLIST
import cz.aaa.unit2026.blocking.DEFAULT_BLOCK_RULES
import cz.aaa.unit2026.blocking.InstalledApp
import cz.aaa.unit2026.monitoring.ActiveApp
import cz.aaa.unit2026.monitoring.ForegroundAppMonitor
import cz.aaa.unit2026.monitoring.MonitorStatus
import cz.aaa.unit2026.tracking.TrackingSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

/**
 * Central state holder connecting the UI to the monitoring and blocking layers.
 *
 * The monitor runs passively from init() for debug screen + seenApps tracking.
 * Blocking only activates during a running, non-paused session with strict mode on.
 */
object FocusSessionState {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var enforcer: BlockingEnforcer? = null
    @Volatile private var timerJob: Job? = null
    private var storage: AppStorage = NoOpAppStorage

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // Storage keys
    private const val KEY_BLACKLIST = "blacklist"
    private const val KEY_BLOCK_RULES = "block_rules"
    private const val KEY_STRICT_MODE = "strict_mode"
    private const val KEY_SESSION_HISTORY = "session_history"
    private const val KEY_APP_USAGE = "app_usage"
    private const val KEY_SEEN_APPS = "seen_apps" // persisted as Map<appId, appName>

    // --- Session history ---
    private val _sessionHistory = MutableStateFlow<List<FocusSessionRecord>>(emptyList())
    val sessionHistory: StateFlow<List<FocusSessionRecord>> = _sessionHistory.asStateFlow()

    private var sessionStartMs = 0L
    private var plannedSeconds = 0L
    private val currentDistractions = mutableListOf<DistractionAttempt>()

    // --- App usage tracking ---
    private val _appUsage = MutableStateFlow<Map<String, AppUsageStat>>(emptyMap())
    val appUsage: StateFlow<Map<String, AppUsageStat>> = _appUsage.asStateFlow()

    private var lastTrackedApp: ActiveApp? = null
    private var lastTrackedStartMs = 0L

    // --- Session state ---
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(0L)
    val remainingSeconds: StateFlow<Long> = _remainingSeconds.asStateFlow()

    private val _durationSeconds = MutableStateFlow(25L * 60)

    // --- Monitor status ---
    private val _monitorStatus = MutableStateFlow(MonitorStatus.IDLE)
    val monitorStatus: StateFlow<MonitorStatus> = _monitorStatus.asStateFlow()

    // --- Passive monitor output ---
    private val _currentApp = MutableStateFlow<ActiveApp?>(null)
    val currentApp: StateFlow<ActiveApp?> = _currentApp.asStateFlow()

    private val _seenApps = MutableStateFlow<Map<String, ActiveApp>>(emptyMap())
    val seenApps: StateFlow<Map<String, ActiveApp>> = _seenApps.asStateFlow()

    // --- Blocking ---
    private val _isStrictMode = MutableStateFlow(true)
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

    fun init(monitor: ForegroundAppMonitor, enforcer: BlockingEnforcer, storage: AppStorage = NoOpAppStorage) {
        this.enforcer = enforcer
        this.storage = storage
        loadAll()
        monitor.start()
        scope.launch {
            monitor.status.collect { _monitorStatus.value = it }
        }
        scope.launch {
            monitor.activeApp.collect { app ->
                _currentApp.value = app

                // Record time spent in the previous app before switching
                val prev = lastTrackedApp
                if (prev != null && prev.appId !in SELF_APP_IDS) {
                    val durationMs = System.currentTimeMillis() - lastTrackedStartMs
                    if (durationMs > 0) {
                        val duringFocus = _isRunning.value && !_isPaused.value
                        val existing = _appUsage.value[prev.appId] ?: AppUsageStat(prev.appId, prev.appName)
                        _appUsage.value = _appUsage.value + (prev.appId to if (duringFocus) {
                            existing.copy(focusMs = existing.focusMs + durationMs)
                        } else {
                            existing.copy(offFocusMs = existing.offFocusMs + durationMs)
                        })
                    }
                }
                lastTrackedApp = app
                lastTrackedStartMs = System.currentTimeMillis()

                if (app.appId !in SELF_APP_IDS) {
                    val prevSize = _seenApps.value.size
                    _seenApps.value = _seenApps.value + (app.appId to app)
                    if (_seenApps.value.size > prevSize) {
                        persistSeenApps()
                    }
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
        // Auto-block apps in default-blocked categories (e.g. Games) that aren't already known
        val autoBlock = apps
            .filter { it.category in AppCategory.DEFAULT_BLOCKED && it.appId !in _blacklist.value }
            .map { it.appId }
            .toSet()
        if (autoBlock.isNotEmpty()) {
            _blacklist.value = _blacklist.value + autoBlock
            persistBlacklist()
        }
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
        persistAppUsage()
    }

    /**
     * Called from the App-level bridge whenever [TrackingClient.sessionState] changes.
     *
     * Keeps [_isRunning] / [_isPaused] in sync with the network session so blocking
     * and distraction tracking work correctly. Also saves [FocusSessionRecord] entries
     * to [sessionHistory] when a session ends, and drives [_remainingSeconds] for
     * the blocking overlay display.
     */
    fun syncFromClient(session: TrackingSession?) {
        val now = System.currentTimeMillis()
        val isActive = session != null
            && session.stoppedAtMs == null
            && session.targetEndAtMs > now

        if (!isActive) {
            if (_isRunning.value && sessionStartMs > 0L) {
                // session == null → client cleared it after manual stop → not completed
                // session.stoppedAtMs != null → manually stopped → not completed
                // session.targetEndAtMs <= now → natural expiry (but handled by countdown job)
                saveSession(completed = false)
            }
            _isRunning.value = false
            _isPaused.value = false
            sessionStartMs = 0L
            _remainingSeconds.value = 0L
            timerJob?.cancel()
            timerJob = null
            enforcer?.unblock()
            _blockedApp.value = null
            return
        }

        checkNotNull(session)

        if (sessionStartMs != session.startedAtMs) {
            // New session adopted — save any in-progress session first
            if (_isRunning.value && sessionStartMs > 0L) {
                saveSession(completed = false)
            }
            sessionStartMs = session.startedAtMs
            plannedSeconds = (session.targetEndAtMs - session.startedAtMs) / 1000
            currentDistractions.clear()
        }

        val nowPaused = session.pausedAtMs != null
        _isRunning.value = true
        _isPaused.value = nowPaused

        if (nowPaused) {
            // Freeze displayed remaining time at the moment of pause
            _remainingSeconds.value = ((session.targetEndAtMs - session.pausedAtMs!!) / 1000)
                .coerceAtLeast(0L)
            timerJob?.cancel()
            timerJob = null
            enforcer?.unblock()
            _blockedApp.value = null
        } else {
            // (Re-)start countdown display derived from targetEndAtMs.
            // Always cancel first so resume after pause picks up the correct time.
            val targetEndAtMs = session.targetEndAtMs
            timerJob?.cancel()
            timerJob = scope.launch {
                while (true) {
                    val remaining = (targetEndAtMs - System.currentTimeMillis()) / 1000
                    _remainingSeconds.value = remaining.coerceAtLeast(0L)
                    if (remaining <= 0L) {
                        // Natural expiry — save history and reset blocking state
                        if (sessionStartMs > 0L) saveSession(completed = true)
                        sessionStartMs = 0L
                        _isRunning.value = false
                        _isPaused.value = false
                        enforcer?.unblock()
                        _blockedApp.value = null
                        break
                    }
                    delay(1000L)
                }
            }
        }
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
        persistSessionHistory()
        persistAppUsage()
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
        persistStrictMode()
    }

    fun addToBlacklist(appId: String) {
        _blacklist.value = _blacklist.value + appId
        persistBlacklist()
    }

    fun removeFromBlacklist(appId: String) {
        _blacklist.value = _blacklist.value - appId
        persistBlacklist()
    }

    fun addAllToBlacklist(appIds: Set<String>) {
        _blacklist.value = _blacklist.value + appIds
        persistBlacklist()
    }

    fun removeAllFromBlacklist(appIds: Set<String>) {
        _blacklist.value = _blacklist.value - appIds
        persistBlacklist()
    }

    fun addBlockRule(rule: BlockRule) {
        _blockRules.value = _blockRules.value + rule
        persistBlockRules()
    }

    fun removeBlockRule(rule: BlockRule) {
        _blockRules.value = _blockRules.value - rule
        persistBlockRules()
    }

    fun toggleBlockRule(rule: BlockRule) {
        _blockRules.value = _blockRules.value.map {
            if (it == rule) it.copy(enabled = !it.enabled) else it
        }
        persistBlockRules()
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

    // --- Persistence ---

    private fun loadAll() {
        runCatching {
            storage.load(KEY_BLACKLIST)?.let { raw ->
                val list = json.decodeFromString(ListSerializer(String.serializer()), raw)
                _blacklist.value = list.toSet()
            }
        }
        runCatching {
            storage.load(KEY_BLOCK_RULES)?.let { raw ->
                _blockRules.value = json.decodeFromString(ListSerializer(BlockRule.serializer()), raw)
            }
        }
        runCatching {
            storage.load(KEY_STRICT_MODE)?.let { raw ->
                _isStrictMode.value = raw.toBoolean()
            }
        }
        runCatching {
            storage.load(KEY_SESSION_HISTORY)?.let { raw ->
                _sessionHistory.value = json.decodeFromString(ListSerializer(FocusSessionRecord.serializer()), raw)
            }
        }
        runCatching {
            storage.load(KEY_APP_USAGE)?.let { raw ->
                _appUsage.value = json.decodeFromString(MapSerializer(String.serializer(), AppUsageStat.serializer()), raw)
            }
        }
        runCatching {
            storage.load(KEY_SEEN_APPS)?.let { raw ->
                val nameMap = json.decodeFromString(MapSerializer(String.serializer(), String.serializer()), raw)
                _seenApps.value = nameMap.mapValues { (id, name) ->
                    ActiveApp(appId = id, appName = name, windowTitle = null, capturedAtMs = 0L)
                }
            }
        }
    }

    private fun persistBlacklist() {
        val snapshot = _blacklist.value.toList()
        scope.launch {
            runCatching { storage.save(KEY_BLACKLIST, json.encodeToString(ListSerializer(String.serializer()), snapshot)) }
        }
    }

    private fun persistBlockRules() {
        val snapshot = _blockRules.value
        scope.launch {
            runCatching { storage.save(KEY_BLOCK_RULES, json.encodeToString(ListSerializer(BlockRule.serializer()), snapshot)) }
        }
    }

    private fun persistStrictMode() {
        val value = _isStrictMode.value.toString()
        scope.launch {
            runCatching { storage.save(KEY_STRICT_MODE, value) }
        }
    }

    private fun persistSessionHistory() {
        val snapshot = _sessionHistory.value
        scope.launch {
            runCatching { storage.save(KEY_SESSION_HISTORY, json.encodeToString(ListSerializer(FocusSessionRecord.serializer()), snapshot)) }
        }
    }

    private fun persistAppUsage() {
        val snapshot = _appUsage.value
        scope.launch {
            runCatching { storage.save(KEY_APP_USAGE, json.encodeToString(MapSerializer(String.serializer(), AppUsageStat.serializer()), snapshot)) }
        }
    }

    private fun persistSeenApps() {
        val nameMap = _seenApps.value.mapValues { it.value.appName }
        scope.launch {
            runCatching { storage.save(KEY_SEEN_APPS, json.encodeToString(MapSerializer(String.serializer(), String.serializer()), nameMap)) }
        }
    }

    private val SELF_APP_IDS = setOf(
        "cz-aaa-unit2026-MainKt",
        "java-lang-Thread",
        "cz.aaa.unit2026",
    )
}
