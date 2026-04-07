package cz.aaa.unit2026

import cz.aaa.unit2026.tracking.*
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.server.testing.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlin.test.*

class TrackingTest {

    private val future get() = System.currentTimeMillis() + 60_000L

    // Wraps a WebSocket session so tests can send/receive from outside the webSocket { } block.
    private class WsSession(
        private val outgoing: kotlinx.coroutines.channels.SendChannel<Frame>,
        private val messages: Channel<ServerMessage>,
    ) {
        suspend fun next(): ServerMessage = messages.receive()
        suspend fun send(msg: ClientMessage) {
            outgoing.send(Frame.Text(trackingJson.encodeToString<ClientMessage>(msg)))
        }
    }

    private fun ApplicationTestBuilder.wsClient() = createClient { install(WebSockets) }

    /**
     * Opens a WebSocket connection and returns a [WsSession] + a [Job] that drives it.
     * The job must be cancelled when the test is done.
     */
    private fun CoroutineScope.openSession(
        client: HttpClient,
        messages: Channel<ServerMessage> = Channel(Channel.UNLIMITED),
    ): Pair<WsSession, Job> {
        var wsSession: WsSession? = null
        val ready = CompletableDeferred<Unit>()
        val job = launch {
            client.webSocket("/ws/tracking") {
                wsSession = WsSession(outgoing, messages)
                ready.complete(Unit)
                for (frame in incoming) {
                    if (frame !is Frame.Text) continue
                    try {
                        messages.send(trackingJson.decodeFromString<ServerMessage>(frame.readText()))
                    } catch (_: Exception) { break }
                }
            }
        }
        runBlocking { ready.await() }
        return Pair(wsSession!!, job)
    }

    @Test
    fun `new client receives SessionState null when no session active`() = testApplication {
        application { module() }
        wsClient().webSocket("/ws/tracking") {
            val frame = incoming.receive() as Frame.Text
            val state = trackingJson.decodeFromString<ServerMessage>(frame.readText())
            assertIs<ServerMessage.SessionState>(state)
            assertNull(state.session)
        }
    }
}
