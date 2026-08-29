package com.duncanbottrill.ridenamer.name

import com.duncanbottrill.ridenamer.model.RideStats
import com.duncanbottrill.ridenamer.model.WeatherSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MinimalNameGeneratorTest {

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
        weather = WeatherSnapshot(9.0, 12.0, 61), // rain, cold
        placeName = "Box Hill",
    )

    private fun words(name: String) = name.split(" ")

    @Test
    fun `word count is honoured exactly`() {
        assertEquals(1, words(MinimalNameGenerator.generate(ride, 1, seed = 1L)).size)
        assertEquals(2, words(MinimalNameGenerator.generate(ride, 2, seed = 1L)).size)
        assertEquals(3, words(MinimalNameGenerator.generate(ride, 3, seed = 1L)).size)
    }

    @Test
    fun `out of range word counts are coerced, not thrown`() {
        assertEquals(1, words(MinimalNameGenerator.generate(ride, 0, seed = 1L)).size)
        assertEquals(1, words(MinimalNameGenerator.generate(ride, -5, seed = 1L)).size)
        assertEquals(3, words(MinimalNameGenerator.generate(ride, 4, seed = 1L)).size)
        assertEquals(3, words(MinimalNameGenerator.generate(ride, 99, seed = 1L)).size)
    }

    @Test
    fun `shorter names are prefixes of the colour and keep the speed word`() {
        val one = MinimalNameGenerator.generate(ride, 1, seed = 7L)
        val two = MinimalNameGenerator.generate(ride, 2, seed = 7L)
        val three = MinimalNameGenerator.generate(ride, 3, seed = 7L)
        // Colour is slot 1 in every length.
        assertEquals(words(one)[0], words(two)[0])
        assertEquals(words(one)[0], words(three)[0])
        // Speed is always last; the time word is what gets dropped.
        assertEquals(words(two)[1], words(three)[2])
    }

    @Test
    fun `is deterministic per seed`() {
        assertEquals(
            MinimalNameGenerator.generate(ride, 3, seed = 5L),
            MinimalNameGenerator.generate(ride, 3, seed = 5L),
        )
    }

    @Test
    fun `different seeds vary the name`() {
        val names = (1L..40L).map { MinimalNameGenerator.generate(ride, 3, seed = it) }.toSet()
        assertTrue("expected variety, got $names", names.size > 1)
    }

    @Test
    fun `words come from the banks for this ride`() {
        val c = RideClassification.of(ride)
        val name = words(MinimalNameGenerator.generate(ride, 3, seed = 3L))
        assertTrue(name[0], MinimalWordBanks.colours(c.condition!!, c.temp)!!.contains(name[0]))
        assertTrue(name[1], MinimalWordBanks.times.getValue(c.time).contains(name[1]))
        assertTrue(name[2], MinimalWordBanks.speeds.getValue(c.speed).contains(name[2]))
    }

    @Test
    fun `missing weather still produces a full length name from the sky palette`() {
        val bare = ride.copy(weather = null)
        val c = RideClassification.of(bare)
        val name = words(MinimalNameGenerator.generate(bare, 3, seed = 2L))
        assertEquals(3, name.size)
        assertTrue(name[0], MinimalWordBanks.skyColours.getValue(c.time).contains(name[0]))
    }

    @Test
    fun `an unrecognised weather code behaves like missing weather`() {
        // WMO code 4 is not mapped, so WeatherCondition.fromWmo returns UNKNOWN.
        val unknown = ride.copy(weather = WeatherSnapshot(9.0, 12.0, 4))
        val c = RideClassification.of(unknown)
        val name = words(MinimalNameGenerator.generate(unknown, 3, seed = 2L))
        assertEquals(3, name.size)
        assertTrue(name[0], MinimalWordBanks.skyColours.getValue(c.time).contains(name[0]))
    }

    @Test
    fun `colour and time words are never identical`() {
        listOf(ride, ride.copy(weather = null)).forEach { stats ->
            (1L..60L).forEach { seed ->
                val name = words(MinimalNameGenerator.generate(stats, 3, seed = seed))
                assertNotEquals(name.joinToString(" "), name[0], name[1])
            }
        }
    }

    @Test
    fun `never produces a blank name`() {
        (1L..30L).forEach { seed ->
            (1..3).forEach { count ->
                assertTrue(MinimalNameGenerator.generate(ride, count, seed = seed).isNotBlank())
            }
        }
    }
}
