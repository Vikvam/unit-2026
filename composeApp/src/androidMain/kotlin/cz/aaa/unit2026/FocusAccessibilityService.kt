package cz.aaa.unit2026

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
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

        val windowTitle = event.text.firstOrNull()?.toString()
            ?.takeIf { it.isNotEmpty() }

        ActiveAppBus.emit(
            ActiveApp(
                appId = packageName,
                appName = packageName.substringAfterLast('.'),
                windowTitle = windowTitle,
                capturedAtMs = System.currentTimeMillis(),
            )
        )
    }

    override fun onInterrupt() = Unit

    private companion object {
        const val SYSTEM_UI_PACKAGE = "com.android.systemui"
    }
}
