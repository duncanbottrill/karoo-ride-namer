package com.duncanbottrill.ridenamer.name

import com.duncanbottrill.ridenamer.model.RideStats
import com.duncanbottrill.ridenamer.model.WeatherCondition
import java.time.Instant
import java.time.ZoneId

/** Discrete buckets the name generator reasons about. */
enum class DistanceBand { SPIN, SHORT, MEDIUM, LONG, EPIC }
enum class ClimbBand { FLAT, ROLLING, HILLY, MOUNTAINOUS }
enum class IntensityBand { CHILL, STEADY, HARD, SAVAGE }
enum class TempBand { FREEZING, COLD, MILD, WARM, SCORCHING }
enum class TimeBand { DAWN, MORNING, MIDDAY, AFTERNOON, EVENING, NIGHT }

/**
 * Turns raw [RideStats] into the handful of categorical signals the templates use.
 * Kept deterministic and dependency-free so it's trivial to unit test.
 */
data class RideClassification(
    val distance: DistanceBand,
    val climb: ClimbBand,
    val intensity: IntensityBand,
    val temp: TempBand?,
    val condition: WeatherCondition?,
    val windy: Boolean,
    val time: TimeBand,
    val fast: Boolean,
) {
    companion object {
        fun of(stats: RideStats, zone: ZoneId = ZoneId.systemDefault()): RideClassification {
            val distance = when {
                stats.distanceKm >= 100 -> DistanceBand.EPIC
                stats.distanceKm >= 60 -> DistanceBand.LONG
                stats.distanceKm >= 30 -> DistanceBand.MEDIUM
                stats.distanceKm >= 12 -> DistanceBand.SHORT
                else -> DistanceBand.SPIN
            }

            val climb = when {
                stats.climbPerKm >= 18 -> ClimbBand.MOUNTAINOUS
                stats.climbPerKm >= 12 -> ClimbBand.HILLY
                stats.climbPerKm >= 6 -> ClimbBand.ROLLING
                else -> ClimbBand.FLAT
            }

            val intensity = classifyIntensity(stats)

            val temp = stats.weather?.let {
                when {
                    it.temperatureC < 2 -> TempBand.FREEZING
                    it.temperatureC < 10 -> TempBand.COLD
                    it.temperatureC < 20 -> TempBand.MILD
                    it.temperatureC < 28 -> TempBand.WARM
                    else -> TempBand.SCORCHING
                }
            }

            val windy = (stats.weather?.windSpeedKmh ?: 0.0) >= 25.0

            val hour = Instant.ofEpochMilli(stats.startEpochMs).atZone(zone).hour
            val time = when (hour) {
                in 4..6 -> TimeBand.DAWN
                in 7..10 -> TimeBand.MORNING
                in 11..13 -> TimeBand.MIDDAY
                in 14..16 -> TimeBand.AFTERNOON
                in 17..20 -> TimeBand.EVENING
                else -> TimeBand.NIGHT
            }

            // "Fast" is relative to terrain: 28+ on the flat, less if it was a climb-fest.
            val fastThreshold = when (climb) {
                ClimbBand.MOUNTAINOUS -> 20.0
                ClimbBand.HILLY -> 24.0
                ClimbBand.ROLLING -> 27.0
                ClimbBand.FLAT -> 30.0
            }
            val fast = stats.avgSpeedKmh >= fastThreshold

            return RideClassification(
                distance, climb, intensity, temp,
                stats.weather?.condition, windy, time, fast,
            )
        }

        private fun classifyIntensity(stats: RideStats): IntensityBand {
            // Prefer Intensity Factor when a power meter + FTP gave us one.
            stats.intensityFactor?.let { ifv ->
                return when {
                    ifv >= 0.95 -> IntensityBand.SAVAGE
                    ifv >= 0.80 -> IntensityBand.HARD
                    ifv >= 0.65 -> IntensityBand.STEADY
                    else -> IntensityBand.CHILL
                }
            }
            // Otherwise blend HR effort with how hard the terrain pushed back.
            val hrFraction = if (stats.avgHr != null && stats.maxHr != null && stats.maxHr > 0)
                stats.avgHr / stats.maxHr else null
            val terrainPush = (stats.climbPerKm / 20.0).coerceIn(0.0, 1.0)
            val speedPush = (stats.avgSpeedKmh / 35.0).coerceIn(0.0, 1.0)
            val score = hrFraction ?: ((terrainPush * 0.6) + (speedPush * 0.4) + 0.25)
            return when {
                score >= 0.85 -> IntensityBand.SAVAGE
                score >= 0.72 -> IntensityBand.HARD
                score >= 0.55 -> IntensityBand.STEADY
                else -> IntensityBand.CHILL
            }
        }
    }
}
