package cz.aaa.unit2026.util

import kotlin.random.Random

/** Current epoch time in milliseconds. Platform-specific. */
expect fun currentTimeMs(): Long

/** Generates a random 32-character hex session ID using Kotlin's common Random. */
fun generateSessionId(): String = buildString(32) {
    val hex = "0123456789abcdef"
    repeat(32) { append(hex[Random.nextInt(hex.length)]) }
}
