package cz.aaa.unit2026

import cz.aaa.unit2026.tracking.KtorTrackingClient
import cz.aaa.unit2026.tracking.TrackingClient

internal actual fun createTrackingClient(): TrackingClient =
    KtorTrackingClient(host = "unit26.jastr.dev", port = SERVER_PORT)
