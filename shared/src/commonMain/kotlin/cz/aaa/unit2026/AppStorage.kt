package cz.aaa.unit2026

interface AppStorage {
    fun load(key: String): String?
    fun save(key: String, value: String)
}

/** No-op for targets where persistence is not applicable (web). */
object NoOpAppStorage : AppStorage {
    override fun load(key: String): String? = null
    override fun save(key: String, value: String) = Unit
}
