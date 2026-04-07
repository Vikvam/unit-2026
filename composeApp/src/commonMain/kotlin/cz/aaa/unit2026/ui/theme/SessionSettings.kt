package cz.aaa.unit2026.ui.theme

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object SessionSettings {
    private val _durationMinutes = MutableStateFlow(25)
    val durationMinutes: StateFlow<Int> = _durationMinutes

    fun setDuration(minutes: Int) {
        _durationMinutes.value = minutes.coerceIn(1, 180)
    }
}
