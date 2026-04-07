package cz.aaa.unit2026

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import cz.aaa.unit2026.ui.theme.OpenJetTracksTheme

data class PermissionStatus(
    val overlay: Boolean,
    val accessibility: Boolean,
)

fun checkPermissions(context: Context): PermissionStatus {
    val overlay = Settings.canDrawOverlays(context)

    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val accessibility = am.getEnabledAccessibilityServiceList(
        AccessibilityServiceInfo.FEEDBACK_ALL_MASK
    ).any { it.resolveInfo.serviceInfo.name == FocusAccessibilityService::class.java.name }

    return PermissionStatus(overlay = overlay, accessibility = accessibility)
}

fun allPermissionsGranted(context: Context): Boolean {
    val status = checkPermissions(context)
    return status.overlay && status.accessibility
}

@Composable
fun PermissionsOnboardingScreen(
    onAllGranted: () -> Unit,
) {
    val context = LocalContext.current
    var status by remember { mutableStateOf(checkPermissions(context)) }

    // Re-check when returning from settings
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        status = checkPermissions(context)
        if (status.overlay && status.accessibility) {
            onAllGranted()
        }
    }

    OpenJetTracksTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(Modifier.height(24.dp))

                Text(
                    text = "Welcome to OpenJetTracks",
                    style = MaterialTheme.typography.headlineMedium,
                )

                Text(
                    text = "To keep you focused, we need a couple of permissions. Here's what each one does and why we need it.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(8.dp))

                PermissionCard(
                    title = "Draw over other apps",
                    description = "Shows the floating timer bubble when you leave the app during a focus session. Without this, you won't see the countdown overlay.",
                    granted = status.overlay,
                    onRequest = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}"),
                        )
                        context.startActivity(intent)
                    },
                )

                PermissionCard(
                    title = "Accessibility service",
                    description = "Detects which app is in the foreground so we can block distracting apps during strict mode. We never read your screen content.",
                    granted = status.accessibility,
                    onRequest = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    },
                )

                Spacer(Modifier.height(16.dp))

                if (status.overlay && status.accessibility) {
                    Button(
                        onClick = onAllGranted,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Continue")
                    }
                } else {
                    Text(
                        text = "Grant all permissions above to continue.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    granted: Boolean,
    onRequest: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (granted)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                if (granted) {
                    Text(
                        text = "Granted",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (!granted) {
                Spacer(Modifier.height(12.dp))
                Button(onClick = onRequest) {
                    Text("Grant permission")
                }
            }
        }
    }
}
