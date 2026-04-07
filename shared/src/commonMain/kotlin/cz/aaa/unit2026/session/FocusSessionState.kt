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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Central state holder connecting the UI to the monitoring and blocking layers.
 *
 * The monitor runs passively from app launch (for debug info + seen apps tracking).
 * Blocking only activates during a session with strict mode on.
 */
object FocusSessionState {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var enforcer: BlockingEnforcer? = null

    // --- Timer ---
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

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
                // Accumulate seen apps (for desktop blacklist management)
                if (app.appId !in SELF_APP_IDS) {
                    _seenApps.value = _seenApps.value + (app.appId to app)
                }
                // Enforce blocking during active sessions with strict mode
                if (_isRunning.value && _isStrictMode.value && app.appId !in SELF_APP_IDS) {
                    if (shouldBlock(app)) {
                        enforcer.block(app)
                        _blockedApp.value = app
                    }
                }
            }
        }
    }

    fun setInstalledApps(apps: List<InstalledApp>) {
        _installedApps.value = apps
    }

    // --- Session controls ---

    fun startSession() {
        _isRunning.value = true
    }

    fun stopSession() {
        _isRunning.value = false
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
