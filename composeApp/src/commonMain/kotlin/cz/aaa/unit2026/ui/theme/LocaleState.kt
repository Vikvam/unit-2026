package cz.aaa.unit2026.ui.theme

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class AppLocale(val code: String?, val displayName: String, val flag: String) {
    System(null, "System", "\uD83C\uDF10"),
    English("en", "English", "\uD83C\uDDEC\uD83C\uDDE7"),
    Czech("cs", "Čeština", "\uD83C\uDDE8\uD83C\uDDFF"),
    Slovak("sk", "Slovenčina", "\uD83C\uDDF8\uD83C\uDDF0"),
    French("fr", "Français", "\uD83C\uDDEB\uD83C\uDDF7"),
    Spanish("es", "Español", "\uD83C\uDDEA\uD83C\uDDF8"),
    Esperanto("eo", "Esperanto", "\uD83D\uDFE2"),
    Japanese("ja", "日本語", "\uD83C\uDDEF\uD83C\uDDF5"),
}

object LocaleState {
    private val _locale = MutableStateFlow(AppLocale.System)
    val locale: StateFlow<AppLocale> = _locale

    fun setLocale(locale: AppLocale) {
        applyLocale(locale.code)
        _locale.value = locale
    }
}
