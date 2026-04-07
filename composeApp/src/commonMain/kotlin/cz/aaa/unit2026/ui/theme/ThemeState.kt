package cz.aaa.unit2026.ui.theme

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ThemeMode { System, Light, Dark }

object ThemeState {
    private val _mode = MutableStateFlow(ThemeMode.System)
    val mode: StateFlow<ThemeMode> = _mode

    fun setMode(mode: ThemeMode) {
        _mode.value = mode
    }
}
