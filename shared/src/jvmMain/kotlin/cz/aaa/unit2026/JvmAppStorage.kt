package cz.aaa.unit2026

import java.io.File

class JvmAppStorage : AppStorage {
    private val dir = File(System.getProperty("user.home"), ".local/share/unit2026")
        .also { it.mkdirs() }

    override fun load(key: String): String? =
        File(dir, "$key.json").takeIf { it.exists() }?.readText()

    override fun save(key: String, value: String) {
        File(dir, "$key.json").writeText(value)
    }
}
