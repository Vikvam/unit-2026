package cz.aaa.unit2026

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform