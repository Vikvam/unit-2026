package cz.aaa.unit2026.tracking

import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.util.concurrent.CopyOnWriteArraySet

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
     * Sends [message] to every connected client except [exclude].
     * Per-socket send errors are swallowed — the disconnect handler calls [leave] independently.
     */
    suspend fun broadcast(message: ServerMessage, exclude: DefaultWebSocketServerSession? = null) {
        val frame = Frame.Text(trackingJson.encodeToString<ServerMessage>(message))
        sessions.forEach { ws ->
            if (ws !== exclude) runCatching { ws.send(frame) }
        }
    }

    /** Sends [message] to a single target session only. */
    suspend fun sendTo(target: DefaultWebSocketServerSession, message: ServerMessage) {
        val frame = Frame.Text(trackingJson.encodeToString<ServerMessage>(message))
        runCatching { target.send(frame) }
    }
}
