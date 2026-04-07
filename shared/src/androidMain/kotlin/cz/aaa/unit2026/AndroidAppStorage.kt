package cz.aaa.unit2026

import java.io.File

class AndroidAppStorage(filesDir: File) : AppStorage {
    private val dir = File(filesDir, "prefs").also { it.mkdirs() }

    override fun load(key: String): String? =
        File(dir, "$key.json").takeIf { it.exists() }?.readText()

    override fun save(key: String, value: String) {
        File(dir, "$key.json").writeText(value)
    }
}
