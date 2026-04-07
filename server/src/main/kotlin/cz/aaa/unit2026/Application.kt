package cz.aaa.unit2026

import cz.aaa.unit2026.tracking.SessionRegistry
import cz.aaa.unit2026.tracking.trackingRoutes
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlin.time.Duration.Companion.seconds

fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val registry = SessionRegistry()

    install(WebSockets) {
        pingPeriod = 30.seconds
        timeout = 60.seconds
    }

    install(ContentNegotiation) {
        json()
    }

    routing {
        get("/") {
            call.respondText("Ktor: ${Greeting().greet()}")
        }
        trackingRoutes(registry)
    }
}