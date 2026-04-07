package cz.aaa.unit2026

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cz.aaa.unit2026.monitoring.AndroidForegroundAppMonitor

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MonitorDemoScreen(AndroidForegroundAppMonitor())
        }
    }
}
