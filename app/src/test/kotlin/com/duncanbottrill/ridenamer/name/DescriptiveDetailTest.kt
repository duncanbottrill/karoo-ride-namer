package com.duncanbottrill.ridenamer.name

import com.duncanbottrill.ridenamer.model.RideStats
import com.duncanbottrill.ridenamer.model.WeatherSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DescriptiveDetailTest {

    /** 61 km, 250 m climb (4.1 m/km -> FLAT), clear and warm, moderate effort. */
    private val ride = RideStats(
        distanceKm = 61.0,
        elevationGainM = 250.0,
        durationSec = 8400.0,
        avgSpeedKmh = 26.0,
        maxSpeedKmh = 50.0,
        avgHr = 140.0,
        maxHr = 185.0,
        startEpochMs = 1_700_000_000_000L,
        endEpochMs = 1_700_000_000_000L,
        weather = WeatherSnapshot(22.0, 8.0, 0), // clear
        placeName = "Box Hill",
    )

    /** 61 km with 950 m of climb -> 15.6 m/km -> HILLY. */
    private val hilly = ride.copy(elevationGainM = 950.0)

    private fun gen(stats: RideStats, detail: DescriptiveDetail, seed: Long = 1L) =
        DescriptiveNameGenerator.generate(stats, detail, seed)

    @Test
    fun `distance and terrain only`() {
        assertEquals("61 km flat", gen(ride, DescriptiveDetail.DISTANCE_TERRAIN))
        assertEquals("61 km hilly", gen(hilly, DescriptiveDetail.DISTANCE_TERRAIN))
    }

    @Test
    fun `with effort adds the intensity word`() {
        // avgHr 140 of a 185 max is 0.757, which lands in IntensityBand.HARD.
        assertEquals("61 km hilly hard", gen(hilly, DescriptiveDetail.WITH_EFFORT))
    }

    @Test
    fun `with weather says sunny for a clear sky`() {
        assertEquals("61 km hilly sunny", gen(hilly, DescriptiveDetail.WITH_WEATHER))
    }

    @Test
    fun `with weather uses the right word for each condition`() {
        fun wordFor(wmo: Int) =
            gen(hilly.copy(weather = WeatherSnapshot(12.0, 8.0, wmo)), DescriptiveDetail.WITH_WEATHER)
                .removePrefix("61 km hilly ")
        assertEquals("cloudy", wordFor(2))
        assertEquals("foggy", wordFor(45))
        assertEquals("wet", wordFor(61))
        assertEquals("snowy", wordFor(71))
        assertEquals("stormy", wordFor(95))
    }

    @Test
    fun `with weather falls back to distance and terrain when there is none`() {
        // Rather than inventing a condition. Also covers an unrecognised WMO code.
        assertEquals("61 km hilly", gen(hilly.copy(weather = null), DescriptiveDetail.WITH_WEATHER))
        assertEquals(
            "61 km hilly",
            gen(hilly.copy(weather = WeatherSnapshot(12.0, 8.0, 4)), DescriptiveDetail.WITH_WEATHER),
        )
    }

    @Test
    fun `compact forms drop the place and the time of day`() {
        DescriptiveDetail.entries.filter { it != DescriptiveDetail.FULL }.forEach { detail ->
            val name = gen(hilly, detail)
            assertTrue(name, !name.contains("Box Hill"))
            assertTrue(name, !name.contains("ride"))
        }
    }

    @Test
    fun `compact forms are fixed, not random`() {
        // Unlike FULL, which picks a template per seed, these must not vary.
        DescriptiveDetail.entries.filter { it != DescriptiveDetail.FULL }.forEach { detail ->
            val names = (1L..30L).map { gen(hilly, detail, seed = it) }.toSet()
            assertEquals("$detail varied: $names", 1, names.size)
        }
    }

    @Test
    fun `full is unchanged and still varies`() {
        val names = (1L..30L).map { gen(hilly, DescriptiveDetail.FULL, seed = it) }.toSet()
        assertTrue("expected variety, got $names", names.size > 1)
        assertTrue(names.any { it.contains("Box Hill") })
    }

    @Test
    fun `full is the default for both the generator and an unknown stored value`() {
        assertEquals(DescriptiveDetail.FULL, DescriptiveDetail.DEFAULT)
        assertEquals(DescriptiveDetail.FULL, DescriptiveDetail.fromName(null))
        assertEquals(DescriptiveDetail.FULL, DescriptiveDetail.fromName("NOT_A_DETAIL"))
        assertEquals(DescriptiveDetail.WITH_WEATHER, DescriptiveDetail.fromName("WITH_WEATHER"))
        assertEquals(
            DescriptiveNameGenerator.generate(hilly, seed = 3L),
            gen(hilly, DescriptiveDetail.FULL, seed = 3L),
        )
    }

    @Test
    fun `dispatcher passes the detail through`() {
        assertEquals(
            gen(hilly, DescriptiveDetail.WITH_WEATHER, seed = 7L),
            generateRideName(hilly, NameStyle.DESCRIPTIVE, detail = DescriptiveDetail.WITH_WEATHER, seed = 7L),
        )
    }

    @Test
    fun `only descriptive advertises a detail setting`() {
        assertEquals(
            setOf(NameStyle.DESCRIPTIVE),
            NameStyle.entries.filter { it.hasDetail }.toSet(),
        )
    }
}
