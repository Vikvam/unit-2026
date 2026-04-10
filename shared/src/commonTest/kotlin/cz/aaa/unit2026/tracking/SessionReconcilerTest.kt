package cz.aaa.unit2026.tracking

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Exhaustive matrix for [SessionReconciler.decide]. Every combination of
 * (local state × server state) that the reconcile path can encounter is
 * represented here. If you add a new state or change the decision rules,
 * update this matrix in lockstep.
 *
 * Naming convention: `<local> + <server> -> <decision>`.
 */
class SessionReconcilerTest {

    private val now = 100_000L

    /**
     * Builds a [TrackingSession] relative to [now]. All offsets are in ms.
     * Default session is active (running, started 5s ago, ends in 25min).
     */
    private fun session(
        id: String = "test-session",
        startedOffset: Long = -5_000,
        endOffset: Long = 25 * 60 * 1000L,
        pausedOffset: Long? = null,
        stoppedOffset: Long? = null,
    ) = TrackingSession(
        sessionId = id,
        startedAtMs = now + startedOffset,
        targetEndAtMs = now + endOffset,
        pausedAtMs = pausedOffset?.let { now + it },
        stoppedAtMs = stoppedOffset?.let { now + it },
    )

    // ---- local=null branch --------------------------------------------

    @Test
    fun `null local + null server -- NoOp`() {
        assertEquals(
            ReconcileDecision.NoOp,
            SessionReconciler.decide(local = null, server = null, nowMs = now),
        )
    }

    @Test
    fun `null local + running server -- AdoptServer`() {
        val server = session(id = "srv")
        assertEquals(
            ReconcileDecision.AdoptServer(server),
            SessionReconciler.decide(local = null, server = server, nowMs = now),
        )
    }

    @Test
    fun `null local + paused server -- AdoptServer`() {
        val server = session(id = "srv", pausedOffset = -1_000)
        assertEquals(
            ReconcileDecision.AdoptServer(server),
            SessionReconciler.decide(local = null, server = server, nowMs = now),
        )
    }

    @Test
    fun `null local + expired server -- NoOp`() {
        val server = session(id = "srv", endOffset = -1_000)
        assertEquals(
            ReconcileDecision.NoOp,
            SessionReconciler.decide(local = null, server = server, nowMs = now),
        )
    }

    @Test
    fun `null local + stopped server -- NoOp (defensive, should be filtered by caller)`() {
        val server = session(id = "srv", stoppedOffset = -1_000)
        assertEquals(
            ReconcileDecision.NoOp,
            SessionReconciler.decide(local = null, server = server, nowMs = now),
        )
    }

    // ---- server=null branch -------------------------------------------

    @Test
    fun `running local + null server -- PushLocal`() {
        val local = session(id = "loc")
        assertEquals(
            ReconcileDecision.PushLocal(local),
            SessionReconciler.decide(local = local, server = null, nowMs = now),
        )
    }

    @Test
    fun `paused local + null server -- PushLocal`() {
        val local = session(id = "loc", pausedOffset = -1_000)
        assertEquals(
            ReconcileDecision.PushLocal(local),
            SessionReconciler.decide(local = local, server = null, nowMs = now),
        )
    }

    @Test
    fun `stopped local + null server -- NoOp (preserve for Report screen)`() {
        val local = session(id = "loc", stoppedOffset = -100)
        assertEquals(
            ReconcileDecision.NoOp,
            SessionReconciler.decide(local = local, server = null, nowMs = now),
        )
    }

    @Test
    fun `expired local + null server -- NoOp`() {
        val local = session(id = "loc", endOffset = -1_000)
        assertEquals(
            ReconcileDecision.NoOp,
            SessionReconciler.decide(local = local, server = null, nowMs = now),
        )
    }

    // ---- both live (conflict) -----------------------------------------

    @Test
    fun `running local + running server -- AdoptServer (server wins)`() {
        val local = session(id = "loc", endOffset = 25 * 60 * 1000L)
        val server = session(id = "srv", endOffset = 30 * 60 * 1000L)
        assertEquals(
            ReconcileDecision.AdoptServer(server),
            SessionReconciler.decide(local = local, server = server, nowMs = now),
        )
    }

    @Test
    fun `paused local + paused server -- AdoptServer`() {
        val local = session(id = "loc", pausedOffset = -2_000)
        val server = session(id = "srv", pausedOffset = -500)
        assertEquals(
            ReconcileDecision.AdoptServer(server),
            SessionReconciler.decide(local = local, server = server, nowMs = now),
        )
    }

    /**
     * Load-bearing test — this is the "resumed while offline" exception that
     * has been a bug on the Ktor side. Keeps us honest on any future refactor.
     */
    @Test
    fun `running local + paused server -- AdoptServerAndPushResume`() {
        val local = session(id = "loc", endOffset = 30 * 60 * 1000L)
        val server = session(id = "srv", pausedOffset = -5_000)
        assertEquals(
            ReconcileDecision.AdoptServerAndPushResume(
                server = server,
                localTargetEndAtMs = local.targetEndAtMs,
            ),
            SessionReconciler.decide(local = local, server = server, nowMs = now),
        )
    }

    @Test
    fun `running local + expired server -- PushLocal`() {
        val local = session(id = "loc")
        val server = session(id = "srv", endOffset = -1_000)
        assertEquals(
            ReconcileDecision.PushLocal(local),
            SessionReconciler.decide(local = local, server = server, nowMs = now),
        )
    }
}
