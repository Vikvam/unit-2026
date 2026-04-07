package cz.aaa.unit2026.blocking

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import cz.aaa.unit2026.FocusAccessibilityService
import cz.aaa.unit2026.MainActivity
import cz.aaa.unit2026.monitoring.ActiveApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AndroidBlockingEnforcer(private val context: Context) : BlockingEnforcer {

    private val _blockedApp = MutableStateFlow<ActiveApp?>(null)
    override val blockedApp: StateFlow<ActiveApp?> = _blockedApp

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        notificationManager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Focus Blocking", NotificationManager.IMPORTANCE_HIGH)
        )
    }

    override fun block(app: ActiveApp) {
        if (_blockedApp.value?.appId == app.appId) return
        _blockedApp.value = app
        FocusAccessibilityService.sendHome()
        showNotification(app)
    }

    override fun unblock() {
        _blockedApp.value = null
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun showNotification(app: ActiveApp) {
        val openApp = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Stay focused.")
            .setContentText("${app.appName} is blocked during your focus session.")
            .setContentIntent(openApp)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private companion object {
        const val CHANNEL_ID = "focus_blocking"
        const val NOTIFICATION_ID = 1
    }
}
