package com.duncanbottrill.ridenamer.name

import com.duncanbottrill.ridenamer.model.RideStats
import com.duncanbottrill.ridenamer.model.WeatherSnapshot

/** The naming styles the user can choose between. */
enum class NameStyle(val label: String, val blurb: String) {
    FUNNY("Funny", "Silly, random names for a laugh"),
    DESCRIPTIVE("Descriptive", "Plain facts: place, distance, effort, weather"),
    MINIMAL("Minimal", "A colour, a time, a pace — in as few words as you like"),
    ;

    companion object {
        fun fromName(name: String?): NameStyle = entries.firstOrNull { it.name == name } ?: FUNNY
    }
}

/**
 * Single entry point: generates a ride name in the chosen [style].
 *
 * [wordCount] applies to [NameStyle.MINIMAL] only; the other styles ignore it.
 */
fun generateRideName(
    stats: RideStats,
    style: NameStyle,
    wordCount: Int = MinimalNameGenerator.DEFAULT_WORDS,
    seed: Long? = null,
): String = when (style) {
    NameStyle.FUNNY -> RideNameGenerator.generate(stats, seed)
    NameStyle.DESCRIPTIVE -> DescriptiveNameGenerator.generate(stats, seed)
    NameStyle.MINIMAL -> MinimalNameGenerator.generate(stats, wordCount, seed)
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
