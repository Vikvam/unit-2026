package cz.aaa.unit2026.blocking

import cz.aaa.unit2026.monitoring.AppCategory

data class InstalledApp(
    val appId: String,
    val appName: String,
    val category: Int = AppCategory.UNDEFINED,
    val iconBytes: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is InstalledApp) return false
        return appId == other.appId
    }

    override fun hashCode(): Int = appId.hashCode()
}
