package cz.aaa.unit2026.tracking

import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-agnostic interface for a connected tracking client.
 *
 * All state mutations ([startSession], [stopSession]) update [sessionState] immediately,
 * even while offline — the UI never blocks on connectivity.
 *
 * JVM implementation (Android + Desktop): [KtorTrackingClient]
 */
interface TrackingClient {

    /** Live view of the current session. Null = no active session. Works offline. */
    val sessionState: StateFlow<TrackingSession?>

    /** Whether the client currently has a live server connection. */
    val isConnected: StateFlow<Boolean>

    /**
     * Starts the connection loop: connects, then reconnects on failure with exponential backoff.
     * Suspends until [disconnect] is called. Launch in a long-lived coroutine scope.
     */
    suspend fun run(deviceId: String)

    /**
     * Starts a focus session. Updates [sessionState] immediately (works offline).
     * If connected, sends the start command to the server so other clients are notified.
     * On next reconnect, any offline-started session is pushed to the server automatically.
     */
    suspend fun startSession(startedAtMs: Long, targetEndAtMs: Long)

    /**
     * Stops the active session early. Updates [sessionState] immediately (works offline).
     * If connected, sends the stop command so other clients are notified.
     */
    suspend fun stopSession()

    /**
     * Pauses the active session. Updates [sessionState] immediately (works offline).
     * No-op if there is no active session or the session is already paused.
     * If connected, broadcasts the pause to other clients.
     */
    suspend fun pauseSession()

    /**
     * Resumes a paused session. Updates [sessionState] immediately (works offline).
     * No-op if there is no active session or the session is not paused.
     * If connected, broadcasts the resume to other clients.
     */
    suspend fun resumeSession()

    /** Terminates the connection loop and closes any open WebSocket. */
    fun disconnect()
}
