package com.duncanbottrill.ridenamer.name

import com.duncanbottrill.ridenamer.model.RideStats
import com.duncanbottrill.ridenamer.model.WeatherCondition
import java.time.ZoneId
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Generates a funny, random, ride-specific name from [RideStats].
 *
 * Fully offline and deterministic for a given seed: pass the ride's end-time as the
 * seed and you'll get the same name every time, or pass a fresh [Random] to reshuffle.
 *
 * It works by classifying the ride, building a weighted pool of candidate templates
 * (weather/climb/intensity-led templates get heavier weights when those signals are
 * strong), then filling the chosen template's word slots at random.
 */
object RideNameGenerator {

    fun generate(stats: RideStats, seed: Long? = null): String {
        val rng = if (seed != null) Random(seed) else Random.Default
        val c = RideClassification.of(stats)
        val place = stats.placeName?.trim()?.takeIf { it.isNotEmpty() }

        val candidates = buildList {
            // --- Always-available, stat-driven templates ---
            add(weighted(3) {
                "${pick(rng, WordBanks.climbAdj[c.climb]!!)} ${pick(rng, WordBanks.distanceNoun[c.distance]!!)}"
            })
            add(weighted(3) {
                "The ${pick(rng, WordBanks.intensityAdj[c.intensity]!!)} ${pick(rng, WordBanks.distanceNoun[c.distance]!!)}"
            })
            add(weighted(2) {
                "${km(stats)}km of ${pick(rng, WordBanks.intensityNoun[c.intensity]!!)}"
            })
            add(weighted(2) {
                "${pick(rng, WordBanks.timeAdj[c.time]!!)} ${pick(rng, WordBanks.intensityNoun[c.intensity]!!)}"
            })
            add(weighted(1) {
                "${pick(rng, WordBanks.distanceNoun[c.distance]!!)}: ${pick(rng, WordBanks.flourish)}"
            })

            // --- Climb-led (heavier when it was actually hilly) ---
            if (c.climb == ClimbBand.HILLY || c.climb == ClimbBand.MOUNTAINOUS) {
                add(weighted(4) {
                    "${meters(stats)}m Up — ${pick(rng, WordBanks.climbPhrase[c.climb]!!).replaceFirstChar { it.uppercase() }}"
                })
                add(weighted(3) {
                    "${pick(rng, WordBanks.climbAdj[c.climb]!!)} & ${pick(rng, WordBanks.intensityAdj[c.intensity]!!)}"
                })
                add(weighted(2) { "Everest in Miniature (${meters(stats)}m)" })
            }
            if (c.climb == ClimbBand.FLAT && c.distance >= DistanceBand.MEDIUM) {
                add(weighted(3) { "${pick(rng, WordBanks.climbAdj[ClimbBand.FLAT]!!)} Power Hour" })
            }

            // --- Weather-led (heavier in spicy conditions) ---
            c.condition?.let { cond ->
                val weight = if (cond == WeatherCondition.CLEAR || cond == WeatherCondition.CLOUDY) 2 else 5
                add(weighted(weight) {
                    "${pick(rng, WordBanks.weatherAdj[cond]!!)} ${pick(rng, WordBanks.distanceNoun[c.distance]!!)}"
                })
                add(weighted(weight) {
                    "${pick(rng, WordBanks.distanceNoun[c.distance]!!)}: ${pick(rng, WordBanks.weatherPhrase[cond]!!).replaceFirstChar { it.uppercase() }}"
                })
            }
            c.temp?.let { t ->
                add(weighted(if (t == TempBand.FREEZING || t == TempBand.SCORCHING) 4 else 1) {
                    "${pick(rng, WordBanks.tempPhrase[t]!!)} ${pick(rng, WordBanks.distanceNoun[c.distance]!!)}"
                })
            }
            if (c.windy) {
                add(weighted(4) {
                    pick(rng, listOf(
                        "The Great Headwind Betrayal",
                        "${pick(rng, WordBanks.distanceNoun[c.distance]!!)} vs. The Wind",
                        "Powered by Spite (and a Headwind)",
                        "Windswept ${pick(rng, WordBanks.distanceNoun[c.distance]!!)}",
                    ))
                })
            }

            // --- Intensity / speed-led ---
            if (c.intensity == IntensityBand.SAVAGE) {
                add(weighted(4) {
                    pick(rng, listOf(
                        "Why Do I Do This",
                        "${pick(rng, WordBanks.intensityNoun[IntensityBand.SAVAGE]!!)}",
                        "Legs: Cancelled",
                        "${km(stats)}km of Regret",
                    ))
                })
            }
            if (c.intensity == IntensityBand.CHILL) {
                add(weighted(3) {
                    pick(rng, listOf(
                        "Just Vibes",
                        "${pick(rng, WordBanks.timeAdj[c.time]!!)} Coffee Run",
                        "Doctor's Orders (Recovery)",
                        "All the Chill, None of the Watts",
                    ))
                })
            }
            if (c.fast) {
                add(weighted(3) {
                    pick(rng, listOf(
                        "Average Speed: Show-Off (${kmh(stats)}km/h)",
                        "Pretending to Have a Train",
                        "Suspiciously Fast ${pick(rng, WordBanks.distanceNoun[c.distance]!!)}",
                    ))
                })
            }

            // --- Time / distance flavour ---
            if (c.time == TimeBand.DAWN || c.time == TimeBand.NIGHT) {
                add(weighted(3) {
                    "${pick(rng, WordBanks.timeAdj[c.time]!!)} ${pick(rng, WordBanks.distanceNoun[c.distance]!!)}"
                })
            }
            if (c.distance == DistanceBand.EPIC) {
                add(weighted(4) {
                    pick(rng, listOf(
                        "${km(stats)}km — Send Help",
                        "The ${pick(rng, WordBanks.distanceNoun[DistanceBand.EPIC]!!)}",
                        "Saddle Sore & Proud (${km(stats)}km)",
                    ))
                })
            }
            if (c.distance == DistanceBand.SPIN) {
                add(weighted(2) {
                    pick(rng, listOf(
                        "Barely a Ride, Honestly",
                        "${km(stats)}km of Plausible Deniability",
                        "The Obligatory Spin",
                    ))
                })
            }

            // --- Location-led (only when we resolved a place name) ---
            if (place != null) {
                add(weighted(3) {
                    "${pick(rng, WordBanks.climbAdj[c.climb]!!)} ${pick(rng, WordBanks.distanceNoun[c.distance]!!)} ${pick(rng, WordBanks.placeConnector)} $place"
                })
                add(weighted(3) { "$place ${pick(rng, WordBanks.intensityNoun[c.intensity]!!)}" })
                add(weighted(2) { "The $place ${pick(rng, WordBanks.distanceNoun[c.distance]!!)}" })
                add(weighted(2) { "Tour de $place" })
                add(weighted(2) {
                    "${pick(rng, WordBanks.timeAdj[c.time]!!)} ${pick(rng, WordBanks.distanceNoun[c.distance]!!)} in $place"
                })
                add(weighted(2) { "${km(stats)}km ${pick(rng, WordBanks.placeConnector)} $place" })
                // Blend location with the standout condition for extra context.
                c.condition?.let { cond ->
                    add(weighted(2) {
                        "$place, ${pick(rng, WordBanks.weatherPhrase[cond]!!)}"
                    })
                }
                if (c.climb == ClimbBand.HILLY || c.climb == ClimbBand.MOUNTAINOUS) {
                    add(weighted(2) { "The Hills of $place (${meters(stats)}m)" })
                }
            }
        }

        if (candidates.isEmpty()) return "A Ride Happened"
        return chooseWeighted(rng, candidates).build().take(96)
    }

    // --- helpers ---

    private class Weighted(val weight: Int, val build: () -> String)

    private fun weighted(weight: Int, build: () -> String) = Weighted(weight, build)

    private fun <T> pick(rng: Random, list: List<T>): T = list[rng.nextInt(list.size)]

    private fun chooseWeighted(rng: Random, items: List<Weighted>): Weighted {
        val total = items.sumOf { it.weight }
        var roll = rng.nextInt(total)
        for (item in items) {
            roll -= item.weight
            if (roll < 0) return item
        }
        return items.last()
    }

    private fun km(stats: RideStats) = stats.distanceKm.roundToInt()
    private fun meters(stats: RideStats) = stats.elevationGainM.roundToInt()
    private fun kmh(stats: RideStats) = stats.avgSpeedKmh.roundToInt()

    /** Convenience for previews / the "shuffle" button in the UI. */
    fun preview(seed: Long = System.nanoTime()): String = generate(
        RideStats(
            distanceKm = 62.0, elevationGainM = 940.0, durationSec = 9000.0,
            avgSpeedKmh = 26.5, maxSpeedKmh = 58.0, avgHr = 148.0, maxHr = 182.0,
            startEpochMs = System.currentTimeMillis(), endEpochMs = System.currentTimeMillis(),
            weather = com.duncanbottrill.ridenamer.model.WeatherSnapshot(7.0, 31.0, 61),
            placeName = "Box Hill",
        ),
        seed,
    )
}
