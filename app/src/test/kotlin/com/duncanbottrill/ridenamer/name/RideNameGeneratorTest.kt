package com.duncanbottrill.ridenamer.name

import com.duncanbottrill.ridenamer.model.RideStats
import com.duncanbottrill.ridenamer.model.WeatherSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RideNameGeneratorTest {

    private fun ride(
        km: Double, gain: Double, avgKmh: Double, weather: WeatherSnapshot? = null,
        place: String? = null,
    ) = RideStats(
        distanceKm = km,
        elevationGainM = gain,
        durationSec = km / avgKmh * 3600,
        avgSpeedKmh = avgKmh,
        maxSpeedKmh = avgKmh * 2,
        startEpochMs = 1_700_000_000_000L,
        endEpochMs = 1_700_000_000_000L,
        weather = weather,
        placeName = place,
    )

    @Test
    fun `produces a non-empty name`() {
        val name = RideNameGenerator.generate(ride(48.0, 900.0, 22.0), seed = 1L)
        assertTrue(name.isNotBlank())
        assertTrue(name.length <= 96)
    }

    @Test
    fun `same seed is deterministic`() {
        val stats = ride(140.0, 3200.0, 21.0, WeatherSnapshot(24.0, 8.0, 0))
        assertEquals(
            RideNameGenerator.generate(stats, seed = 42L),
            RideNameGenerator.generate(stats, seed = 42L),
        )
    }

    @Test
    fun `varies across seeds`() {
        val stats = ride(60.0, 700.0, 26.0, WeatherSnapshot(9.0, 31.0, 61))
        val names = (0L until 50L).map { RideNameGenerator.generate(stats, seed = it) }.toSet()
        // With weather + wind + distance templates there should be plenty of variety.
        assertTrue("Expected variety, got: $names", names.size >= 8)
    }

    @Test
    fun `includes the place name across some seeds when provided`() {
        val stats = ride(64.0, 920.0, 24.0, WeatherSnapshot(18.0, 10.0, 0), place = "Box Hill")
        val names = (0L until 50L).map { RideNameGenerator.generate(stats, seed = it) }
        assertTrue(
            "Expected at least a few names to mention the place, got: ${names.toSet()}",
            names.count { it.contains("Box Hill") } >= 5,
        )
    }

    @Test
    fun `omits location templates when no place is known`() {
        // Should never invent a place — just shouldn't crash and stays non-blank.
        val name = RideNameGenerator.generate(ride(40.0, 400.0, 25.0, place = null), seed = 3L)
        assertTrue(name.isNotBlank())
    }

    @Test
    fun `does not crash for any band combination`() {
        val distances = listOf(5.0, 20.0, 45.0, 80.0, 130.0)
        val climbs = listOf(0.0, 300.0, 900.0, 2500.0)
        val codes = listOf(0, 3, 45, 61, 75, 95)
        for (d in distances) for (c in climbs) for (code in codes) {
            val name = RideNameGenerator.generate(
                ride(d, c, 25.0, WeatherSnapshot(15.0, 10.0, code)), seed = 7L,
            )
            assertTrue(name.isNotBlank())
        }
    }
}
