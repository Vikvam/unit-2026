package cz.aaa.unit2026.tracking

import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.SerializationException
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("TrackingRoutes")

fun Route.trackingRoutes(registry: SessionRegistry) {
    webSocket("/ws/tracking") {
        registry.join(this)
        val remoteAddress = call.request.local.remoteAddress
        var clientTag = remoteAddress
        log.info("Client connected [{}]. Total: {}", clientTag, registry.sessions.size)

        try {
            // Immediately send current state so the client can reconcile local state
            registry.sendTo(this, ServerMessage.SessionState(registry.currentSession()))

            for (frame in incoming) {
                if (frame !is Frame.Text) continue

                val text = frame.readText()
                log.debug("WS << [{}]: {}", clientTag, text)
                val message = try {
                    trackingJson.decodeFromString<ClientMessage>(text)
                } catch (e: SerializationException) {
                    log.warn("Malformed client message from [{}]: {}", clientTag, e.message)
                    continue
                }

                when (message) {
                    is ClientMessage.Register -> {
                        clientTag = "${message.deviceId}@$remoteAddress"
                        log.info("Device registered [{}]", clientTag)
                    }

                    is ClientMessage.SessionStart -> {
                        val started = registry.startSession(message.startedAtMs, message.targetEndAtMs)
                        if (started != null) {
                            log.info("Session started [{}] sessionId={}", clientTag, started.sessionId)
                            registry.broadcast(ServerMessage.SessionStarted(started))
                        } else {
                            log.debug("SessionStart rejected [{}] — session already active", clientTag)
                            registry.sendTo(this, ServerMessage.SessionState(registry.currentSession()))
                        }
                    }

                    is ClientMessage.SessionStop -> {
                        val stopped = registry.stopSession()
                        if (stopped != null) {
                            log.info("Session stopped [{}] sessionId={}", clientTag, stopped.sessionId)
                            registry.broadcast(ServerMessage.SessionStopped(stopped))
                        } else {
                            log.debug("SessionStop ignored [{}] — no active session", clientTag)
                            registry.sendTo(this, ServerMessage.SessionState(null))
                        }
                    }

                    is ClientMessage.SessionPause -> {
                        val paused = registry.pauseSession()
                        if (paused != null) {
                            log.info("Session paused [{}] sessionId={}", clientTag, paused.sessionId)
                            registry.broadcast(ServerMessage.SessionPaused(paused))
                        } else {
                            log.debug("SessionPause ignored [{}] — no active session or already paused", clientTag)
                            registry.sendTo(this, ServerMessage.SessionState(registry.currentSession()))
                        }
                    }

                    is ClientMessage.SessionResume -> {
                        val resumed = registry.resumeSession(message.extendedTargetEndAtMs)
                        if (resumed != null) {
                            log.info("Session resumed [{}] sessionId={}", clientTag, resumed.sessionId)
                            registry.broadcast(ServerMessage.SessionResumed(resumed))
                        } else {
                            log.debug("SessionResume ignored [{}] — no paused session", clientTag)
                            registry.sendTo(this, ServerMessage.SessionState(registry.currentSession()))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            log.error("WebSocket error [{}]: {}", clientTag, e.message, e)
        } finally {
            registry.leave(this)
            log.info("Client disconnected [{}]. Total: {}", clientTag, registry.sessions.size)
        }
    }
}
