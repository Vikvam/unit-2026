package cz.aaa.unit2026

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import cz.aaa.unit2026.monitoring.ActiveApp
import cz.aaa.unit2026.monitoring.ActiveAppBus

class FocusAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 100
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString()
            ?.takeIf { it.isNotEmpty() }
            ?.takeIf { it != SYSTEM_UI_PACKAGE }
            ?: return

        val windowTitle = resolveWindowTitle(packageName, event)

        ActiveAppBus.emit(
            ActiveApp(
                appId = packageName,
                appName = packageName.substringAfterLast('.'),
                windowTitle = windowTitle,
                capturedAtMs = System.currentTimeMillis(),
            )
        )
    }

    private fun resolveWindowTitle(packageName: String, event: AccessibilityEvent): String? {
        // For known browsers, query the URL bar node directly from the window tree
        val urlBarId = BROWSER_URL_BAR_IDS[packageName]
        if (urlBarId != null) {
            val url = rootInActiveWindow?.findUrl(urlBarId)
            if (!url.isNullOrEmpty()) return url
        }
        // Fall back to event text (works for some apps)
        return event.text.firstOrNull()?.toString()?.takeIf { it.isNotEmpty() }
    }

    private fun AccessibilityNodeInfo.findUrl(viewId: String): String? {
        return findAccessibilityNodeInfosByViewId(viewId)
            .firstOrNull()
            ?.text
            ?.toString()
            ?.takeIf { it.isNotEmpty() }
    }

    override fun onInterrupt() = Unit

    private companion object {
        const val SYSTEM_UI_PACKAGE = "com.android.systemui"

        // Map of browser package → view ID of the URL/domain bar
        val BROWSER_URL_BAR_IDS = mapOf(
            "org.mozilla.fenix"         to "org.mozilla.fenix:id/mozac_browser_toolbar_url_view",
            "org.mozilla.firefox"       to "org.mozilla.firefox:id/mozac_browser_toolbar_url_view",
            "org.mozilla.firefox_beta"  to "org.mozilla.firefox_beta:id/mozac_browser_toolbar_url_view",
            "com.android.chrome"        to "com.android.chrome:id/url_bar",
            "com.chrome.beta"           to "com.chrome.beta:id/url_bar",
            "org.chromium.chrome"       to "org.chromium.chrome:id/url_bar",
        )
    }
}
