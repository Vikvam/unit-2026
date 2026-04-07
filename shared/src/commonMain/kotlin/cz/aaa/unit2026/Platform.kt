package cz.aaa.unit2026

interface Platform {
    val name: String
    val isAndroid: Boolean get() = false
    val isDesktop: Boolean get() = false
}

expect fun getPlatform(): Platform