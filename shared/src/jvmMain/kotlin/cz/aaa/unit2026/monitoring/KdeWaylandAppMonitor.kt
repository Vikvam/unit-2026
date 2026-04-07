package cz.aaa.unit2026.monitoring

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File

private const val PLUGIN_NAME = "openjetracks_monitor"
private const val LOG_PREFIX = "ACTIVE_WINDOW:"

/**
 * Monitors the foreground app on KDE Plasma 6 / Wayland via KWin scripting.
 *
 * Loads a KWin JavaScript that connects to workspace.windowActivated and emits
 * lines to journald via print(). We tail journald to parse those lines.
 *
 * Requires: qdbus, journalctl (present on any KDE install).
 */
class KdeWaylandAppMonitor(
    private val scope: CoroutineScope,
) : ForegroundAppMonitor {

    private val _activeApp = MutableSharedFlow<ActiveApp>(replay = 1)
    override val activeApp: Flow<ActiveApp> = _activeApp.distinctUntilChanged()

    private var tailJob: Job? = null
    private val scriptFile: File by lazy { writeScriptToTemp() }

    override fun start() {
        try {
            loadKwinScript()
        } catch (e: Exception) {
            // qdbus not available or KWin scripting unavailable — monitor degrades gracefully
            return
        }
        tailJob = scope.launch(Dispatchers.IO) {
            tailJournald()
        }
    }

    override fun stop() {
        tailJob?.cancel()
        try {
            unloadKwinScript()
            scriptFile.delete()
        } catch (_: Exception) {
            // best-effort cleanup
        }
    }

    private fun loadKwinScript() {
        qdbus("org.kde.kwin.Scripting.loadScript", scriptFile.absolutePath, PLUGIN_NAME)
        qdbus("org.kde.kwin.Scripting.start")
    }

    private fun unloadKwinScript() {
        qdbus("org.kde.kwin.Scripting.unloadScript", PLUGIN_NAME)
    }

    private suspend fun tailJournald() {
        val process = ProcessBuilder(
            "journalctl", "-f", "-n", "0", "_COMM=kwin_wayland"
        ).redirectErrorStream(true).start()

        try {
            process.inputStream.bufferedReader().forEachLine { line ->
                if (line.contains(LOG_PREFIX)) {
                    parseLine(line)?.let { _activeApp.tryEmit(it) }
                }
            }
        } finally {
            process.destroy()
        }
    }

    private fun parseLine(line: String): ActiveApp? {
        val payload = line.substringAfter(LOG_PREFIX, missingDelimiterValue = "")
            .trim()
            .takeIf { it.isNotEmpty() } ?: return null

        val parts = payload.split("|")
        if (parts.size < 2) return null

        val caption = parts[0]
        val resourceClass = parts[1]
        val pid = parts.getOrNull(2)?.toIntOrNull()
        val geometry = runCatching {
            WindowGeometry(
                x = parts[3].toInt(),
                y = parts[4].toInt(),
                width = parts[5].toInt(),
                height = parts[6].toInt(),
                scale = parts.getOrNull(7)?.toFloat() ?: 1f,
            )
        }.getOrNull()

        return ActiveApp(
            appId = resourceClass,
            appName = resolveAppName(resourceClass, pid),
            windowTitle = caption.takeIf { it.isNotEmpty() },
            capturedAtMs = System.currentTimeMillis(),
            geometry = geometry,
        )
    }

    /** Best-effort human-readable name: use /proc cmdline, fall back to resourceClass. */
    private fun resolveAppName(resourceClass: String, pid: Int?): String {
        if (pid != null) {
            val cmdline = File("/proc/$pid/cmdline").takeIf { it.exists() }
                ?.readText()
                ?.split("\u0000")
                ?.firstOrNull { it.isNotEmpty() }
                ?.substringAfterLast("/")
            if (!cmdline.isNullOrEmpty()) return cmdline
        }
        return resourceClass
    }

    private fun qdbus(method: String, vararg args: String) {
        ProcessBuilder(
            "qdbus", "org.kde.KWin", "/Scripting", method, *args
        ).start().waitFor()
    }

    private fun writeScriptToTemp(): File {
        val file = File.createTempFile("openjetracks_kwin_", ".js")
        file.writeText(KWIN_SCRIPT)
        return file
    }
}

private val KWIN_SCRIPT = """
var trackedWindow = null;

function emit(w) {
    var g = w.frameGeometry;
    var scale = 1;
    try { scale = w.output.devicePixelRatio; } catch(e) {}
    print("$LOG_PREFIX" + w.caption + "|" + w.resourceClass + "|" + w.pid + "|" + g.x + "|" + g.y + "|" + g.width + "|" + g.height + "|" + scale);
}

function trackWindow(w) {
    if (trackedWindow) {
        try { trackedWindow.captionChanged.disconnect(onCaptionChanged); } catch(e) {}
        try { trackedWindow.frameGeometryChanged.disconnect(onGeometryChanged); } catch(e) {}
    }
    trackedWindow = w;
    if (w) {
        w.captionChanged.connect(onCaptionChanged);
        w.frameGeometryChanged.connect(onGeometryChanged);
    }
}

function onCaptionChanged() {
    var w = workspace.activeWindow;
    if (w) emit(w);
}

function onGeometryChanged() {
    var w = workspace.activeWindow;
    if (w) emit(w);
}

workspace.windowActivated.connect(function(w) {
    if (w) emit(w);
    trackWindow(w);
});

var w = workspace.activeWindow;
if (w) {
    emit(w);
    trackWindow(w);
}
""".trimIndent()
