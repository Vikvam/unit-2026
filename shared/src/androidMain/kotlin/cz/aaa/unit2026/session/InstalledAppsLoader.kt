package cz.aaa.unit2026.session

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import cz.aaa.unit2026.blocking.InstalledApp
import cz.aaa.unit2026.monitoring.AppCategory
import java.io.ByteArrayOutputStream

fun loadInstalledApps(packageManager: PackageManager): List<InstalledApp> {
    return packageManager
        .getInstalledApplications(PackageManager.GET_META_DATA)
        .filter { packageManager.getLaunchIntentForPackage(it.packageName) != null }
        .map { info ->
            val icon = try {
                packageManager.getApplicationIcon(info).toBytes()
            } catch (_: Exception) {
                null
            }
            InstalledApp(
                appId = info.packageName,
                appName = packageManager.getApplicationLabel(info).toString(),
                category = AppCategory.resolve(info.packageName, info.category),
                iconBytes = icon,
            )
        }
        .sortedBy { it.appName }
}

private fun Drawable.toBytes(size: Int = 48): ByteArray? {
    val bitmap = when (this) {
        is BitmapDrawable -> bitmap
        else -> {
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            setBounds(0, 0, size, size)
            draw(canvas)
            bmp
        }
    }
    return ByteArrayOutputStream().use { stream ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 80, stream)
        stream.toByteArray()
    }
}
