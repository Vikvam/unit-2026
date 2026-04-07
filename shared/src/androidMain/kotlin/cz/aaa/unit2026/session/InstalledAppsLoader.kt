package cz.aaa.unit2026.session

import android.content.pm.PackageManager
import cz.aaa.unit2026.blocking.InstalledApp
import cz.aaa.unit2026.monitoring.AppCategory

fun loadInstalledApps(packageManager: PackageManager): List<InstalledApp> {
    return packageManager
        .getInstalledApplications(PackageManager.GET_META_DATA)
        .filter { packageManager.getLaunchIntentForPackage(it.packageName) != null }
        .map { info ->
            InstalledApp(
                appId = info.packageName,
                appName = packageManager.getApplicationLabel(info).toString(),
                category = AppCategory.resolve(info.packageName, info.category),
            )
        }
        .sortedBy { it.appName }
}
