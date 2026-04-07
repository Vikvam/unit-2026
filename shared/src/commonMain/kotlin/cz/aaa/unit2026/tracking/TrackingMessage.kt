package cz.aaa.unit2026.tracking

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// ── Client → Server ──────────────────────────────────────────────────────────

@Serializable
sealed class ClientMessage {

    @Serializable
    @SerialName("register")
    data class Register(val deviceId: String) : ClientMessage()

    @Serializable
    @SerialName("sessionStart")
    data class SessionStart(
        val startedAtMs: Long,
        val targetEndAtMs: Long,
    ) : ClientMessage()

    @Serializable
    @SerialName("sessionStop")
    data object SessionStop : ClientMessage()

    @Serializable
    @SerialName("sessionPause")
    data object SessionPause : ClientMessage()

    @Serializable
    @SerialName("sessionResume")
    data class SessionResume(
        /** targetEndAtMs extended by the pause duration so no focus time is lost. */
        val extendedTargetEndAtMs: Long,
    ) : ClientMessage()
}

// ── Server → Client ──────────────────────────────────────────────────────────

@Serializable
sealed class ServerMessage {

    /** Broadcast to all clients when a new session starts. */
    @Serializable
    @SerialName("sessionStarted")
    data class SessionStarted(val session: TrackingSession) : ServerMessage()

    /**
     * Broadcast to all clients when a session is stopped early (manual stop only).
     * Not sent when [TrackingSession.targetEndAtMs] is reached — clients handle that locally.
     */
    @Serializable
    @SerialName("sessionStopped")
    data class SessionStopped(val session: TrackingSession) : ServerMessage()

    /** Broadcast to all clients when the active session is paused. */
    @Serializable
    @SerialName("sessionPaused")
    data class SessionPaused(val session: TrackingSession) : ServerMessage()

    /** Broadcast to all clients when the active session is resumed from pause. */
    @Serializable
    @SerialName("sessionResumed")
    data class SessionResumed(val session: TrackingSession) : ServerMessage()

    /**
     * Sent to a newly connected client so it can reconcile local state with the server.
     * Also used as feedback when a start/stop command is rejected.
     * [session] is null when no session is active.
     */
    @Serializable
    @SerialName("sessionState")
    data class SessionState(val session: TrackingSession?) : ServerMessage()
}

// ── Shared JSON codec ─────────────────────────────────────────────────────────

/**
 * Canonical Json instance for tracking messages. Use on both server and client.
 *
 * Wire format example: {"type":"sessionStart","startedAtMs":…,"targetEndAtMs":…}
 *
 * IMPORTANT: always encode as the sealed parent type to include the "type" discriminator:
 *   trackingJson.encodeToString<ClientMessage>(msg)   ✓
 *   trackingJson.encodeToString(msg)                  ✗ (omits discriminator for concrete types)
 */
val trackingJson = Json {
    classDiscriminator = "type"
    ignoreUnknownKeys = true   // forward-compat: old clients survive new server fields
    encodeDefaults = false     // don't emit null optional fields
}
