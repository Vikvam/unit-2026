package cz.aaa.unit2026.ui.theme

import cz.aaa.unit2026.AppStorage
import cz.aaa.unit2026.NoOpAppStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ThemeMode { System, Light, Dark }

enum class ColorTheme {
    Sage, Lavender, Rose, Ocean, Amber, Slate,
}

object ThemeState {
    private const val KEY = "theme_mode"
    private const val COLOR_KEY = "color_theme"
    private var storage: AppStorage = NoOpAppStorage

    private val _mode = MutableStateFlow(ThemeMode.System)
    val mode: StateFlow<ThemeMode> = _mode

    private val _colorTheme = MutableStateFlow(ColorTheme.Sage)
    val colorTheme: StateFlow<ColorTheme> = _colorTheme

    fun init(storage: AppStorage) {
        this.storage = storage
        val saved = storage.load(KEY)
        if (saved != null) {
            runCatching { _mode.value = ThemeMode.valueOf(saved) }
        }
        val savedColor = storage.load(COLOR_KEY)
        if (savedColor != null) {
            runCatching { _colorTheme.value = ColorTheme.valueOf(savedColor) }
        }
    }

    fun setMode(mode: ThemeMode) {
        _mode.value = mode
        runCatching { storage.save(KEY, mode.name) }
    }

    fun setColorTheme(theme: ColorTheme) {
        _colorTheme.value = theme
        runCatching { storage.save(COLOR_KEY, theme.name) }
    }
}
