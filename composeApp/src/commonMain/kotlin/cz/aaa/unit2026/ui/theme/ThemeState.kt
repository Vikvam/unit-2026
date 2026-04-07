package cz.aaa.unit2026.ui.theme

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ThemeMode { System, Light, Dark }

enum class ColorTheme {
    Sage, Lavender, Rose, Ocean, Amber, Slate,
}

object ThemeState {
    private val _mode = MutableStateFlow(ThemeMode.System)
    val mode: StateFlow<ThemeMode> = _mode

    private val _colorTheme = MutableStateFlow(ColorTheme.Sage)
    val colorTheme: StateFlow<ColorTheme> = _colorTheme

    fun setMode(mode: ThemeMode) {
        _mode.value = mode
    }

    fun setColorTheme(theme: ColorTheme) {
        _colorTheme.value = theme
    }
}
