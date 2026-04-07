package cz.aaa.unit2026.ui.theme

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class BackgroundTheme(val displayKey: String) {
    Minimal("minimal"),
    Stars("stars"),
    Forest("forest"),
    Ocean("ocean"),
}

object BackgroundThemeState {
    private val _theme = MutableStateFlow(BackgroundTheme.Minimal)
    val theme: StateFlow<BackgroundTheme> = _theme

    fun setTheme(theme: BackgroundTheme) {
        _theme.value = theme
    }
}
