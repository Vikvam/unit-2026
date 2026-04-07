package cz.aaa.unit2026

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import cz.aaa.unit2026.blocking.AndroidBlockingEnforcer
import cz.aaa.unit2026.monitoring.AndroidForegroundAppMonitor
import cz.aaa.unit2026.session.FocusSessionState
import cz.aaa.unit2026.session.loadInstalledApps

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not, we proceed either way */ }

    private val requestOverlayPermission = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* user returns from Settings — permission state is checked live via canDrawOverlays */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Request notification permission (Android 13+)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Request overlay permission if not granted
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"),
            )
            requestOverlayPermission.launch(intent)
        }

        FocusSessionState.init(
            monitor = AndroidForegroundAppMonitor(),
            enforcer = AndroidBlockingEnforcer(applicationContext),
        )
        FocusSessionState.setInstalledApps(loadInstalledApps(packageManager))

        setContent { App() }
    }

    override fun onStop() {
        super.onStop()
        // App went to background — show bubble if session is active and we have permission
        if (FocusSessionState.isRunning.value && Settings.canDrawOverlays(this)) {
            val intent = Intent(this, TimerBubbleService::class.java)
            startForegroundService(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        // App came to foreground — remove bubble
        val intent = Intent(this, TimerBubbleService::class.java)
        stopService(intent)
    }
}
