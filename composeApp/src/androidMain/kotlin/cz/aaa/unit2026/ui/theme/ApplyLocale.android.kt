package cz.aaa.unit2026.ui.theme

import java.util.Locale

actual fun applyLocale(languageCode: String?) {
    val locale = if (languageCode != null) Locale.forLanguageTag(languageCode) else Locale.getDefault()
    Locale.setDefault(locale)
}
