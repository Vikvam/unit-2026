package cz.aaa.unit2026

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import cz.aaa.unit2026.blocking.AndroidBlockingEnforcer
import cz.aaa.unit2026.monitoring.AndroidForegroundAppMonitor

class MainActivity : ComponentActivity() {

    private val enforcer by lazy { AndroidBlockingEnforcer(applicationContext) }

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not, we proceed either way */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            MonitorDemoScreen(AndroidForegroundAppMonitor(), enforcer)
        }
    }
}
