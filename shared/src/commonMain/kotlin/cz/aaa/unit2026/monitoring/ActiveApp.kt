package cz.aaa.unit2026.monitoring

data class ActiveApp(
    /** Platform-specific app identifier. Package name on Android, resourceClass/exe name on Desktop. */
    val appId: String,
    /** Human-readable display name. */
    val appName: String,
    /** Window title — meaningful on Desktop (e.g. "YouTube — Firefox"), null on Android. */
    val windowTitle: String?,
    /** Epoch milliseconds at time of capture. */
    val capturedAtMs: Long,
    /** Window geometry — Desktop only, null on Android. */
    val geometry: WindowGeometry? = null,
    /** App category — Android only (AppCategory constants), null on Desktop. */
    val category: Int? = null,
)

data class WindowGeometry(val x: Int, val y: Int, val width: Int, val height: Int, val scale: Float = 1f)
