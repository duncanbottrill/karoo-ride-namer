package com.duncanbottrill.ridenamer.name

import com.duncanbottrill.ridenamer.model.RideStats
import com.duncanbottrill.ridenamer.model.WeatherSnapshot

/** The two naming styles the user can choose between. */
enum class NameStyle(val label: String, val blurb: String) {
    FUNNY("Funny", "Silly, random names for a laugh"),
    DESCRIPTIVE("Descriptive", "Plain facts: place, distance, effort, weather"),
    ;

    companion object {
        fun fromName(name: String?): NameStyle = entries.firstOrNull { it.name == name } ?: FUNNY
    }
}

/** Single entry point: generates a ride name in the chosen [style]. */
fun generateRideName(stats: RideStats, style: NameStyle, seed: Long? = null): String = when (style) {
    NameStyle.FUNNY -> RideNameGenerator.generate(stats, seed)
    NameStyle.DESCRIPTIVE -> DescriptiveNameGenerator.generate(stats, seed)
}

/** A representative ride used for the in-app preview/shuffle. */
val SAMPLE_RIDE_STATS = RideStats(
    distanceKm = 62.0,
    elevationGainM = 940.0,
    durationSec = 9000.0,
    avgSpeedKmh = 26.5,
    maxSpeedKmh = 58.0,
    avgHr = 148.0,
    maxHr = 182.0,
    startEpochMs = System.currentTimeMillis(),
    endEpochMs = System.currentTimeMillis(),
    weather = WeatherSnapshot(7.0, 31.0, 61),
    placeName = "Box Hill",
)
