package cz.aaa.unit2026.blocking

import cz.aaa.unit2026.monitoring.AppCategory

data class InstalledApp(
    val appId: String,
    val appName: String,
    val category: Int = AppCategory.UNDEFINED,
)
