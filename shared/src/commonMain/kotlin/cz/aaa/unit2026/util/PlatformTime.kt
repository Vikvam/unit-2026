package cz.aaa.unit2026.util

import kotlin.random.Random

/** Current epoch time in milliseconds. Platform-specific. */
expect fun currentTimeMs(): Long

/**
 * Generates a random UUID v4 string (canonical dashed form), e.g.
 * `f47ac10b-58cc-4372-a567-0e02b2c3d479`. 128 bits of entropy from Kotlin's
 * common Random. The format matches Postgres's `uuid` type so the same value
 * can serve as both the local session identifier and the DB primary key.
 */
fun generateSessionId(): String {
    val hex = "0123456789abcdef"
    val bytes = ByteArray(16) { Random.nextInt(256).toByte() }
    // Set version (4) and variant (RFC 4122) bits.
    bytes[6] = ((bytes[6].toInt() and 0x0f) or 0x40).toByte()
    bytes[8] = ((bytes[8].toInt() and 0x3f) or 0x80).toByte()
    return buildString(36) {
        for (i in 0 until 16) {
            val b = bytes[i].toInt() and 0xff
            append(hex[b ushr 4])
            append(hex[b and 0x0f])
            if (i == 3 || i == 5 || i == 7 || i == 9) append('-')
        }
    }
}
