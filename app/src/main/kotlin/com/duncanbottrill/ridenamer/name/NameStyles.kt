package com.duncanbottrill.ridenamer.name

import com.duncanbottrill.ridenamer.model.RideStats
import com.duncanbottrill.ridenamer.model.WeatherSnapshot

/** The naming styles the user can choose between. */
enum class NameStyle(
    val label: String,
    val blurb: String,
    /** Whether this style lets the user choose 1, 2 or 3 words. Drives the UI selector. */
    val hasWordCount: Boolean = false,
) {
    FUNNY("Funny", "Silly, random names for a laugh"),
    DESCRIPTIVE("Descriptive", "Plain facts: place, distance, effort, weather"),
    MINIMAL("Minimal", "A colour, a time, a pace — in as few words as you like", hasWordCount = true),
    RANDOM("Random", "Pure nonsense: unrelated words, nothing to do with the ride", hasWordCount = true),
    ;

    companion object {
        fun fromName(name: String?): NameStyle = entries.firstOrNull { it.name == name } ?: FUNNY
    }
}

/**
 * Single entry point: generates a ride name in the chosen [style].
 *
 * [wordCount] applies to [NameStyle.MINIMAL] and [NameStyle.RANDOM], which each keep their
 * own stored count — see [com.duncanbottrill.ridenamer.data.RideNamerStore.wordCountFor].
 * The other two styles ignore it.
 */
fun generateRideName(
    stats: RideStats,
    style: NameStyle,
    wordCount: Int = WordCount.DEFAULT,
    seed: Long? = null,
): String = when (style) {
    NameStyle.FUNNY -> RideNameGenerator.generate(stats, seed)
    NameStyle.DESCRIPTIVE -> DescriptiveNameGenerator.generate(stats, seed)
    NameStyle.MINIMAL -> MinimalNameGenerator.generate(stats, wordCount, seed)
    // Random ignores the ride entirely — that's the point of it.
    NameStyle.RANDOM -> RandomNameGenerator.generate(wordCount, seed)
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
