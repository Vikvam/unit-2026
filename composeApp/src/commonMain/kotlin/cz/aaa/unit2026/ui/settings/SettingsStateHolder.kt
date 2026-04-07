package cz.aaa.unit2026.ui.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SettingsUiState(
    val isStrictModeEnabled: Boolean = false,
    val isDarkThemeEnabled: Boolean = false,
)

/** In-memory settings holder. Persisting to disk is a future task. */
class SettingsStateHolder {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun onStrictModeChanged(enabled: Boolean) {
        _uiState.update { it.copy(isStrictModeEnabled = enabled) }
    }

    fun onDarkThemeChanged(enabled: Boolean) {
        _uiState.update { it.copy(isDarkThemeEnabled = enabled) }
    }
}
