package cz.aaa.unit2026

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import cz.aaa.unit2026.AndroidAppStorage
import cz.aaa.unit2026.blocking.AndroidBlockingEnforcer
import cz.aaa.unit2026.monitoring.AndroidForegroundAppMonitor
import cz.aaa.unit2026.session.FocusSessionState
import cz.aaa.unit2026.session.loadInstalledApps
import cz.aaa.unit2026.ui.theme.LocaleState
import cz.aaa.unit2026.ui.theme.ThemeState

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not, we proceed either way */ }

    private var initialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Notification permission (Android 13+) — can be asked via dialog
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        showContent()
    }

    override fun onResume() {
        super.onResume()
        // Re-evaluate after returning from settings
        showContent()
    }

    private fun showContent() {
        if (allPermissionsGranted(this)) {
            initSessionIfNeeded()
            setContent { App() }
        } else {
            setContent {
                PermissionsOnboardingScreen(
                    onAllGranted = {
                        initSessionIfNeeded()
                        setContent { App() }
                    },
                )
            }
        }
    }

    private fun initSessionIfNeeded() {
        if (initialized) return
        initialized = true
        val storage = AndroidAppStorage(filesDir)
        ThemeState.init(storage)
        LocaleState.init(storage)
        FocusSessionState.init(
            monitor = AndroidForegroundAppMonitor(),
            enforcer = AndroidBlockingEnforcer(applicationContext),
            storage = storage,
        )
        FocusSessionState.setInstalledApps(loadInstalledApps(packageManager))
    }

    override fun onStop() {
        super.onStop()
        if (FocusSessionState.isRunning.value && Settings.canDrawOverlays(this)) {
            val intent = Intent(this, TimerBubbleService::class.java)
            startForegroundService(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, TimerBubbleService::class.java)
        stopService(intent)
    }
}
