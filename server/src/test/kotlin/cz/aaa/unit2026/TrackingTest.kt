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

    @Test
    fun `sessionStart broadcasts SessionStarted to all clients`() = testApplication {
        application { module() }
        val messagesA = Channel<ServerMessage>(Channel.UNLIMITED)
        val messagesB = Channel<ServerMessage>(Channel.UNLIMITED)

        coroutineScope {
            val (sessionA, jobA) = openSession(wsClient(), messagesA)
            val (sessionB, jobB) = openSession(wsClient(), messagesB)

            // Consume initial SessionState on both
            assertIs<ServerMessage.SessionState>(messagesA.receive())
            assertIs<ServerMessage.SessionState>(messagesB.receive())

            // Client A starts a session
            val endMs = future
            sessionA.send(ClientMessage.SessionStart(System.currentTimeMillis(), endMs))

            val fromA = messagesA.receive()
            val fromB = messagesB.receive()
            assertIs<ServerMessage.SessionStarted>(fromA)
            assertIs<ServerMessage.SessionStarted>(fromB)
            assertEquals(fromA.session.sessionId, fromB.session.sessionId)
            assertEquals(endMs, fromA.session.targetEndAtMs)

            jobA.cancel(); jobB.cancel()
        }
    }

    @Test
    fun `sessionStop broadcasts SessionStopped to all clients`() = testApplication {
        application { module() }
        val messagesA = Channel<ServerMessage>(Channel.UNLIMITED)
        val messagesB = Channel<ServerMessage>(Channel.UNLIMITED)

        coroutineScope {
            val (sessionA, jobA) = openSession(wsClient(), messagesA)
            val (sessionB, jobB) = openSession(wsClient(), messagesB)

            assertIs<ServerMessage.SessionState>(messagesA.receive())
            assertIs<ServerMessage.SessionState>(messagesB.receive())

            sessionA.send(ClientMessage.SessionStart(System.currentTimeMillis(), future))
            val started = assertIs<ServerMessage.SessionStarted>(messagesA.receive())
            assertIs<ServerMessage.SessionStarted>(messagesB.receive())

            // Stop from B
            sessionB.send(ClientMessage.SessionStop)
            val stoppedA = assertIs<ServerMessage.SessionStopped>(messagesA.receive())
            val stoppedB = assertIs<ServerMessage.SessionStopped>(messagesB.receive())
            assertEquals(started.session.sessionId, stoppedA.session.sessionId)
            assertEquals(started.session.sessionId, stoppedB.session.sessionId)
            assertNotNull(stoppedA.session.stoppedAtMs)

            jobA.cancel(); jobB.cancel()
        }
    }

    @Test
    fun `late-joining client receives current active session`() = testApplication {
        application { module() }
        val messagesA = Channel<ServerMessage>(Channel.UNLIMITED)

        coroutineScope {
            val (sessionA, jobA) = openSession(wsClient(), messagesA)
            assertIs<ServerMessage.SessionState>(messagesA.receive())

            sessionA.send(ClientMessage.SessionStart(System.currentTimeMillis(), future))
            assertIs<ServerMessage.SessionStarted>(messagesA.receive())

            // Late-joining client
            val messagesB = Channel<ServerMessage>(Channel.UNLIMITED)
            val (_, jobB) = openSession(wsClient(), messagesB)
            val state = assertIs<ServerMessage.SessionState>(messagesB.receive())
            assertNotNull(state.session)

            jobA.cancel(); jobB.cancel()
        }
    }

    @Test
    fun `starting session while one is active returns current state to requester`() = testApplication {
        application { module() }
        val messages = Channel<ServerMessage>(Channel.UNLIMITED)

        coroutineScope {
            val (session, job) = openSession(wsClient(), messages)
            assertIs<ServerMessage.SessionState>(messages.receive())

            session.send(ClientMessage.SessionStart(System.currentTimeMillis(), future))
            assertIs<ServerMessage.SessionStarted>(messages.receive())

            // Try to start another — should get current state back
            session.send(ClientMessage.SessionStart(System.currentTimeMillis(), future))
            val response = assertIs<ServerMessage.SessionState>(messages.receive())
            assertNotNull(response.session)

            job.cancel()
        }
    }
}
