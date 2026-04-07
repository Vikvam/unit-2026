package cz.aaa.unit2026.session

data class AppUsageStat(
    val appId: String,
    val appName: String,
    val focusMs: Long = 0L,       // time spent while a session is running
    val offFocusMs: Long = 0L,    // time spent outside sessions
) {
    val totalMs: Long get() = focusMs + offFocusMs
    val totalMinutes: Int get() = (totalMs / 60_000).toInt()
    val focusMinutes: Int get() = (focusMs / 60_000).toInt()
    val offFocusMinutes: Int get() = (offFocusMs / 60_000).toInt()

    /** Fraction of time spent during focus sessions (0–1). */
    val focusFraction: Float get() = if (totalMs > 0) focusMs.toFloat() / totalMs else 0f
}
