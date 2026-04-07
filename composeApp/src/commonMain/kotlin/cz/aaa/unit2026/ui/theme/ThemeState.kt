package cz.aaa.unit2026.ui.theme

import cz.aaa.unit2026.AppStorage
import cz.aaa.unit2026.NoOpAppStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ThemeMode { System, Light, Dark }

object ThemeState {
    private const val KEY = "theme_mode"
    private var storage: AppStorage = NoOpAppStorage

    private val _mode = MutableStateFlow(ThemeMode.System)
    val mode: StateFlow<ThemeMode> = _mode

    fun init(storage: AppStorage) {
        this.storage = storage
        val saved = storage.load(KEY)
        if (saved != null) {
            runCatching { _mode.value = ThemeMode.valueOf(saved) }
        }
    }

    fun setMode(mode: ThemeMode) {
        _mode.value = mode
        runCatching { storage.save(KEY, mode.name) }
    }
}
