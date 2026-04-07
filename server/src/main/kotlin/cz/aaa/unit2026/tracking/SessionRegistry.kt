package cz.aaa.unit2026.tracking

import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.CopyOnWriteArraySet

private val log = LoggerFactory.getLogger("SessionRegistry")

/**
 * Thread-safe in-memory store for the single active session and all live WebSocket connections.
 *
 * All session mutations go through [mutex] so transitions are atomic relative to broadcasts.
 * [sessions] uses [CopyOnWriteArraySet] so broadcast iteration never blocks concurrent connects/disconnects.
 */
class SessionRegistry {

    private val mutex = Mutex()
    private var activeSession: TrackingSession? = null

    val sessions: CopyOnWriteArraySet<DefaultWebSocketServerSession> = CopyOnWriteArraySet()

    fun join(session: DefaultWebSocketServerSession) { sessions.add(session) }
    fun leave(session: DefaultWebSocketServerSession) { sessions.remove(session) }

    /**
     * Returns the active session if it has not yet reached [TrackingSession.targetEndAtMs].
     * Clears expired sessions lazily (no background scheduler needed).
     */
    suspend fun currentSession(): TrackingSession? = mutex.withLock {
        val session = activeSession ?: return@withLock null
        if (System.currentTimeMillis() >= session.targetEndAtMs) {
            activeSession = null
            null
        } else {
            session
        }
    }

    /**
     * Starts a new session with the timestamps provided by the initiating client.
     * Returns the created [TrackingSession], or null if a non-expired session already exists.
     */
    suspend fun startSession(startedAtMs: Long, targetEndAtMs: Long): TrackingSession? = mutex.withLock {
        val existing = activeSession
        if (existing != null && System.currentTimeMillis() < existing.targetEndAtMs) {
            return@withLock null   // conflict: session already active
        }
        val session = TrackingSession(
            sessionId = UUID.randomUUID().toString(),
            startedAtMs = startedAtMs,
            targetEndAtMs = targetEndAtMs,
        )
        activeSession = session
        session
    }

    /**
     * Stops the current session early by recording [TrackingSession.stoppedAtMs].
     * Returns the completed session, or null if no session was active.
     */
    suspend fun stopSession(): TrackingSession? = mutex.withLock {
        val current = activeSession ?: return@withLock null
        val stopped = current.copy(stoppedAtMs = System.currentTimeMillis())
        activeSession = null
        stopped
    }

    /**
     * Pauses the current session by setting [TrackingSession.pausedAtMs].
     * Returns the updated session, or null if no session is active or it is already paused.
     */
    suspend fun pauseSession(): TrackingSession? = mutex.withLock {
        val current = activeSession ?: return@withLock null
        if (current.pausedAtMs != null) return@withLock null   // already paused
        val paused = current.copy(pausedAtMs = System.currentTimeMillis())
        activeSession = paused
        paused
    }

    /**
     * Resumes a paused session by clearing [TrackingSession.pausedAtMs].
     * Returns the updated session, or null if no session is active or it is not paused.
     */
    suspend fun resumeSession(): TrackingSession? = mutex.withLock {
        val current = activeSession ?: return@withLock null
        if (current.pausedAtMs == null) return@withLock null   // not paused
        val resumed = current.copy(pausedAtMs = null)
        activeSession = resumed
        resumed
    }

    /**
     * Sends [message] to every connected client except [exclude].
     * Per-socket send errors are swallowed — the disconnect handler calls [leave] independently.
     */
    suspend fun broadcast(message: ServerMessage, exclude: DefaultWebSocketServerSession? = null) {
        val text = trackingJson.encodeToString<ServerMessage>(message)
        log.debug("WS >> broadcast ({}): {}", sessions.size, text)
        // Create a fresh Frame per recipient — Ktor's Frame buffer is consumed on first send.
        sessions.forEach { ws ->
            if (ws !== exclude) runCatching { ws.send(Frame.Text(text)) }
        }
    }

    /** Sends [message] to a single target session only. */
    suspend fun sendTo(target: DefaultWebSocketServerSession, message: ServerMessage) {
        val text = trackingJson.encodeToString<ServerMessage>(message)
        log.debug("WS >> sendTo: {}", text)
        runCatching { target.send(Frame.Text(text)) }
    }
}
