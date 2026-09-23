package com.duncanbottrill.ridenamer.name

import com.duncanbottrill.ridenamer.model.RideStats
import com.duncanbottrill.ridenamer.model.WeatherCondition
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * A plain, informative name built from the ride's location, distance, intensity, terrain
 * and weather — e.g. "Morning hard 64 km hilly ride around Box Hill in the rain" or
 * "Box Hill · 64 km · hilly · hard · wet". The less-funny counterpart to [RideNameGenerator].
 *
 * [DescriptiveDetail] trims it down: the compact forms drop the place, the time of day and
 * the sentence scaffolding, leaving "64 km hilly", "64 km hilly hard" or "64 km hilly wet".
 *
 * Deterministic for a given seed; missing pieces (no place / no weather) are dropped cleanly.
 */
object DescriptiveNameGenerator {

    fun generate(
        stats: RideStats,
        detail: DescriptiveDetail = DescriptiveDetail.DEFAULT,
        seed: Long? = null,
    ): String {
        val rng = if (seed != null) Random(seed) else Random.Default
        val c = RideClassification.of(stats)
        val place = stats.placeName?.trim()?.takeIf { it.isNotEmpty() }
        val dist = "${stats.distanceKm.roundToInt()} km"

        val intensity = when (c.intensity) {
            IntensityBand.CHILL -> "easy"
            IntensityBand.STEADY -> "steady"
            IntensityBand.HARD -> "hard"
            IntensityBand.SAVAGE -> "very hard"
        }
        val climb = when (c.climb) {
            ClimbBand.FLAT -> "flat"
            ClimbBand.ROLLING -> "rolling"
            ClimbBand.HILLY -> "hilly"
            ClimbBand.MOUNTAINOUS -> "mountainous"
        }
        val time = when (c.time) {
            TimeBand.DAWN -> "early-morning"
            TimeBand.MORNING -> "morning"
            TimeBand.MIDDAY -> "midday"
            TimeBand.AFTERNOON -> "afternoon"
            TimeBand.EVENING -> "evening"
            TimeBand.NIGHT -> "night"
        }
        val weatherPhrase = when (c.condition) {
            WeatherCondition.CLEAR -> "in the sunshine"
            WeatherCondition.CLOUDY -> "under grey skies"
            WeatherCondition.FOG -> "in the fog"
            WeatherCondition.DRIZZLE -> "in the drizzle"
            WeatherCondition.RAIN -> "in the rain"
            WeatherCondition.SNOW -> "in the snow"
            WeatherCondition.THUNDER -> "in a storm"
            WeatherCondition.UNKNOWN, null -> null
        }
        val weatherWord = when (c.condition) {
            WeatherCondition.CLEAR -> "sunny"
            WeatherCondition.CLOUDY -> "cloudy"
            WeatherCondition.FOG -> "foggy"
            WeatherCondition.DRIZZLE -> "drizzly"
            WeatherCondition.RAIN -> "wet"
            WeatherCondition.SNOW -> "snowy"
            WeatherCondition.THUNDER -> "stormy"
            WeatherCondition.UNKNOWN, null -> null
        }

        // The compact forms are fixed, not drawn from templates — the whole point of asking
        // for "61 km hilly" is that it comes back the same shape every time.
        if (detail != DescriptiveDetail.FULL) {
            val third = when (detail) {
                DescriptiveDetail.WITH_EFFORT -> intensity
                // No usable weather leaves the short form as distance + terrain rather than
                // inventing a condition.
                DescriptiveDetail.WITH_WEATHER -> weatherWord
                else -> null
            }
            return listOfNotNull(dist, climb, third).joinToString(" ")
                .replaceFirstChar { it.uppercase() }
        }

        val templates = listOf<() -> String>(
            // Sentence with time-of-day.
            { sentence("$time $intensity $dist $climb ride", place?.let { "around $it" }, weatherPhrase) },
            // Stat label.
            { listOfNotNull(place, dist, climb, intensity, weatherWord).joinToString(" · ") },
            // Intensity-led sentence.
            { sentence("$intensity $dist $climb ride", place?.let { "from $it" }, weatherPhrase) },
        )

        val name = templates[rng.nextInt(templates.size)]().trim()
        return name.replaceFirstChar { it.uppercase() }.take(96)
    }

    private fun sentence(vararg parts: String?): String =
        parts.filter { !it.isNullOrBlank() }.joinToString(" ").trim()
}
