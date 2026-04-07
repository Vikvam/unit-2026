package cz.aaa.unit2026

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import cz.aaa.unit2026.monitoring.ActiveApp
import cz.aaa.unit2026.monitoring.ActiveAppBus
import cz.aaa.unit2026.monitoring.AppCategory

class FocusAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        instance = this
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 100
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString()
            ?.takeIf { it.isNotEmpty() }
            ?.takeIf { it != SYSTEM_UI_PACKAGE }
            ?: return

        val windowTitle = resolveWindowTitle(packageName, event)

        val (appName, category) = resolveAppInfo(packageName)

        ActiveAppBus.emit(
            ActiveApp(
                appId = packageName,
                appName = appName,
                windowTitle = windowTitle,
                capturedAtMs = System.currentTimeMillis(),
                category = category,
            )
        )
    }

    private fun resolveWindowTitle(packageName: String, event: AccessibilityEvent): String? {
        val urlBarId = BROWSER_URL_BAR_IDS[packageName]
        if (urlBarId != null) {
            val url = rootInActiveWindow?.findUrl(urlBarId)
            if (!url.isNullOrEmpty()) return url
        }
        return event.text.firstOrNull()?.toString()?.takeIf { it.isNotEmpty() }
    }

    private fun AccessibilityNodeInfo.findUrl(viewId: String): String? {
        return findAccessibilityNodeInfosByViewId(viewId)
            .firstOrNull()
            ?.text
            ?.toString()
            ?.takeIf { it.isNotEmpty() }
    }

    /** Returns Pair(humanLabel, category) for the given package. */
    private fun resolveAppInfo(packageName: String): Pair<String, Int> {
        return try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            val label = packageManager.getApplicationLabel(info).toString()
            val category = info.category.takeIf { it >= 0 } ?: AppCategory.UNDEFINED
            label to category
        } catch (e: PackageManager.NameNotFoundException) {
            packageName.substringAfterLast('.') to AppCategory.UNDEFINED
        }
    }

    override fun onInterrupt() = Unit

    companion object {
        var instance: FocusAccessibilityService? = null
            private set

        fun sendHome() {
            instance?.performGlobalAction(GLOBAL_ACTION_HOME)
        }

        const val SYSTEM_UI_PACKAGE = "com.android.systemui"

        val BROWSER_URL_BAR_IDS = mapOf(
            "org.mozilla.fenix"        to "org.mozilla.fenix:id/mozac_browser_toolbar_url_view",
            "org.mozilla.firefox"      to "org.mozilla.firefox:id/mozac_browser_toolbar_url_view",
            "org.mozilla.firefox_beta" to "org.mozilla.firefox_beta:id/mozac_browser_toolbar_url_view",
            "com.android.chrome"       to "com.android.chrome:id/url_bar",
            "com.chrome.beta"          to "com.chrome.beta:id/url_bar",
            "org.chromium.chrome"      to "org.chromium.chrome:id/url_bar",
        )
    }
}
