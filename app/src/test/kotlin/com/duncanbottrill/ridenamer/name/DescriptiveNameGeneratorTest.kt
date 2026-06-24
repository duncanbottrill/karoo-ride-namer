package com.duncanbottrill.ridenamer.name

import com.duncanbottrill.ridenamer.model.RideStats
import com.duncanbottrill.ridenamer.model.WeatherSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DescriptiveNameGeneratorTest {

    private val ride = RideStats(
        distanceKm = 64.0,
        elevationGainM = 920.0,
        durationSec = 9600.0,
        avgSpeedKmh = 24.0,
        maxSpeedKmh = 52.0,
        avgHr = 150.0,
        maxHr = 185.0,
        startEpochMs = 1_700_000_000_000L,
        endEpochMs = 1_700_000_000_000L,
        weather = WeatherSnapshot(9.0, 12.0, 61), // rain
        placeName = "Box Hill",
    )

    @Test
    fun `descriptive name mentions distance and place`() {
        val name = DescriptiveNameGenerator.generate(ride, seed = 1L)
        assertTrue(name, name.contains("64 km"))
        assertTrue(name, name.contains("Box Hill"))
    }

    @Test
    fun `descriptive name is deterministic per seed`() {
        assertEquals(
            DescriptiveNameGenerator.generate(ride, seed = 5L),
            DescriptiveNameGenerator.generate(ride, seed = 5L),
        )
    }

    @Test
    fun `handles missing place and weather without crashing`() {
        val bare = ride.copy(placeName = null, weather = null)
        val name = DescriptiveNameGenerator.generate(bare, seed = 2L)
        assertTrue(name.isNotBlank())
        assertTrue(name, name.contains("64 km"))
    }

    @Test
    fun `dispatcher routes to the selected style`() {
        val funny = generateRideName(ride, NameStyle.FUNNY, seed = 9L)
        val descriptive = generateRideName(ride, NameStyle.DESCRIPTIVE, seed = 9L)
        assertEquals(RideNameGenerator.generate(ride, 9L), funny)
        assertEquals(DescriptiveNameGenerator.generate(ride, 9L), descriptive)
    }
}
