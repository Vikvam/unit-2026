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
        log.info("Client connected. Total: ${registry.sessions.size}")

        try {
            // Immediately send current state so the client can reconcile local state
            registry.sendTo(this, ServerMessage.SessionState(registry.currentSession()))

            for (frame in incoming) {
                if (frame !is Frame.Text) continue

                val message = try {
                    trackingJson.decodeFromString<ClientMessage>(frame.readText())
                } catch (e: SerializationException) {
                    log.warn("Malformed client message: ${e.message}")
                    continue
                }

                when (message) {
                    is ClientMessage.Register -> {
                        log.info("Device registered: ${message.deviceId}")
                    }

                    is ClientMessage.SessionStart -> {
                        val started = registry.startSession(message.startedAtMs, message.targetEndAtMs)
                        if (started != null) {
                            log.info("Session started: ${started.sessionId}")
                            registry.broadcast(ServerMessage.SessionStarted(started))
                        } else {
                            log.debug("SessionStart rejected — session already active")
                            registry.sendTo(this, ServerMessage.SessionState(registry.currentSession()))
                        }
                    }

                    is ClientMessage.SessionStop -> {
                        val stopped = registry.stopSession()
                        if (stopped != null) {
                            log.info("Session stopped: ${stopped.sessionId}")
                            registry.broadcast(ServerMessage.SessionStopped(stopped))
                        } else {
                            log.debug("SessionStop ignored — no active session")
                            registry.sendTo(this, ServerMessage.SessionState(null))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            log.error("WebSocket error: ${e.message}", e)
        } finally {
            registry.leave(this)
            log.info("Client disconnected. Total: ${registry.sessions.size}")
        }
    }
}
