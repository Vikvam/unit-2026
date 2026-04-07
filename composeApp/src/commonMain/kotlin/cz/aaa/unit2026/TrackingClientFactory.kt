package cz.aaa.unit2026

import cz.aaa.unit2026.tracking.TrackingClient

/** Returns a platform-appropriate [TrackingClient] instance. */
internal expect fun createTrackingClient(): TrackingClient
