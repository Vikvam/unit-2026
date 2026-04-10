package cz.aaa.unit2026.tracking

/**
 * Pure decision about how to reconcile a locally-held [TrackingSession] with
 * whatever is currently in the remote source of truth. Execution of the
 * decision (mutating local state, pushing updates) is the caller's job — this
 * type only describes *what* should happen.
 */
internal sealed interface ReconcileDecision {
    /** Replace local state with the server row verbatim. */
    data class AdoptServer(val server: TrackingSession) : ReconcileDecision

    /**
     * Adopt the server row *and* push a resume update back to the server
     * carrying the locally-computed extended target end time. This is the
     * "I resumed while offline but the server is still paused" exception —
     * without it, other devices would never learn the user intended to resume.
     */
    data class AdoptServerAndPushResume(
        val server: TrackingSession,
        val localTargetEndAtMs: Long,
    ) : ReconcileDecision

    /** Server has nothing live; push our local session so it becomes the authoritative row. */
    data class PushLocal(val local: TrackingSession) : ReconcileDecision

    /** Nothing to do; preserve current local state (includes stopped/finished rows for the Report screen). */
    data object NoOp : ReconcileDecision
}

/**
 * The single source of truth for reconciliation policy across the sync layer.
 *
 * Pure function of its three arguments — no IO, no side effects, no clocks.
 * Tested exhaustively in `SessionReconcilerTest`. Any change to the behavior
 * here should be accompanied by a new or updated test case.
 *
 * Callers must already have filtered out stopped server rows (the DB query
 * for reconciliation uses `WHERE stopped_at_ms IS NULL`); the `stoppedAtMs`
 * check below is defensive belt-and-braces.
 */
internal object SessionReconciler {

    fun decide(
        local: TrackingSession?,
        server: TrackingSession?,
        nowMs: Long,
    ): ReconcileDecision {
        // Server has a live session — adopt it.
        if (server != null && server.stoppedAtMs == null && server.targetEndAtMs > nowMs) {
            // Exception: we resumed locally while offline but the server is still
            // paused. Adopt the server row for identity/timestamps, then push our
            // locally-computed resume back so other devices catch up.
            return if (local != null && local.pausedAtMs == null && server.pausedAtMs != null) {
                ReconcileDecision.AdoptServerAndPushResume(
                    server = server,
                    localTargetEndAtMs = local.targetEndAtMs,
                )
            } else {
                ReconcileDecision.AdoptServer(server)
            }
        }
        // Server has nothing live. Push our local session if it is itself active.
        if (local != null && local.stoppedAtMs == null && local.targetEndAtMs > nowMs) {
            return ReconcileDecision.PushLocal(local)
        }
        // Nothing actionable anywhere. Preserve whatever local state exists
        // (stopped sessions are kept for the Report screen).
        return ReconcileDecision.NoOp
    }
}
