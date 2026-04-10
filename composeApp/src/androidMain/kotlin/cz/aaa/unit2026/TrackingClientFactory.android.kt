package cz.aaa.unit2026

import cz.aaa.unit2026.tracking.SupabaseTrackingClient
import cz.aaa.unit2026.tracking.TrackingClient

internal actual fun createTrackingClient(storage: AppStorage): TrackingClient =
    SupabaseTrackingClient(storage)