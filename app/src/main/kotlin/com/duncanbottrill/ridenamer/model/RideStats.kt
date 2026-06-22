package com.duncanbottrill.ridenamer.model

import kotlinx.serialization.Serializable

/**
 * Everything we manage to collect about a finished ride. All the optional fields
 * may be null if the relevant sensor wasn't connected (e.g. no power meter).
 */
@Serializable
data class RideStats(
    val distanceKm: Double,
    val elevationGainM: Double,
    /** Recording (moving) time in seconds. */
    val durationSec: Double,
    val avgSpeedKmh: Double,
    val maxSpeedKmh: Double,
    val avgHr: Double? = null,
    val maxHr: Double? = null,
    val avgPower: Double? = null,
    /** Intensity Factor (NP / FTP) for the ride, if power + FTP were available. */
    val intensityFactor: Double? = null,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val weather: WeatherSnapshot? = null,
    /** Human-readable place near the ride's midpoint, e.g. "Box Hill" — null if not resolved. */
    val placeName: String? = null,
) {
    /** Average metres climbed per km — the cleanest "how hilly was it" signal. */
    val climbPerKm: Double
        get() = if (distanceKm > 0.5) elevationGainM / distanceKm else 0.0

    companion object {
        val EMPTY = RideStats(
            distanceKm = 0.0,
            elevationGainM = 0.0,
            durationSec = 0.0,
            avgSpeedKmh = 0.0,
            maxSpeedKmh = 0.0,
            startEpochMs = 0L,
            endEpochMs = 0L,
        )
    }
}
