package cz.aaa.unit2026.tracking

import kotlinx.serialization.Serializable

/**
 * Immutable snapshot of a focus session.
 *
 * [targetEndAtMs] is set by the initiating client. All clients auto-stop their countdown
 * locally when this time is reached — no server broadcast is needed for natural expiry.
 * [stoppedAtMs] is only set when a client explicitly stops the session early.
 *
 * To extend: add nullable fields with defaults — old clients will ignore unknown JSON keys.
 */
@Serializable
data class TrackingSession(
    val sessionId: String,
    val startedAtMs: Long,
    val targetEndAtMs: Long,
    val stoppedAtMs: Long? = null,
    // Future extension: add nullable fields with defaults here
    val label: String? = null,
)
