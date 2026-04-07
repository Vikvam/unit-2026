package cz.aaa.unit2026.util

@JsFun("() => Date.now()")
private external fun jsDateNow(): Double

actual fun currentTimeMs(): Long = jsDateNow().toLong()
