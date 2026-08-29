package com.duncanbottrill.ridenamer.name

import com.duncanbottrill.ridenamer.model.RideStats
import org.junit.Assert.assertEquals
import org.junit.Test

class RideClassificationTest {

    /** A 50 km ride whose elevation picks the climb band and whose speed we vary. */
    private fun ride(avgSpeedKmh: Double, elevationGainM: Double) = RideStats(
        distanceKm = 50.0,
        elevationGainM = elevationGainM,
        durationSec = 7200.0,
        avgSpeedKmh = avgSpeedKmh,
        maxSpeedKmh = avgSpeedKmh + 20.0,
        startEpochMs = 1_700_000_000_000L,
        endEpochMs = 1_700_000_000_000L,
    )

    private val flat = 100.0        // 2 m/km  -> ClimbBand.FLAT,        threshold 30.0
    private val mountainous = 1_000.0 // 20 m/km -> ClimbBand.MOUNTAINOUS, threshold 20.0

    @Test
    fun `speed bands span the flat threshold`() {
        assertEquals(SpeedBand.CRAWL, RideClassification.of(ride(15.0, flat)).speed)   // ratio 0.50
        assertEquals(SpeedBand.AMBLE, RideClassification.of(ride(24.0, flat)).speed)   // ratio 0.80
        assertEquals(SpeedBand.CRUISE, RideClassification.of(ride(28.0, flat)).speed)  // ratio 0.93
        assertEquals(SpeedBand.SWIFT, RideClassification.of(ride(30.0, flat)).speed)   // ratio 1.00
        assertEquals(SpeedBand.FLYING, RideClassification.of(ride(40.0, flat)).speed)  // ratio 1.33
    }

    @Test
    fun `the same speed rates faster on a mountainous ride`() {
        // 24 km/h is a gentle amble on the flat and flying up a mountain.
        assertEquals(SpeedBand.AMBLE, RideClassification.of(ride(24.0, flat)).speed)
        assertEquals(SpeedBand.FLYING, RideClassification.of(ride(24.0, mountainous)).speed)
    }

    @Test
    fun `speed band boundaries are inclusive at the lower edge`() {
        // 0.65 exactly -> AMBLE, not CRAWL (30.0 * 0.65 = 19.5)
        assertEquals(SpeedBand.AMBLE, RideClassification.of(ride(19.5, flat)).speed)
        // 1.15 exactly -> FLYING, not SWIFT (30.0 * 1.15 = 34.5)
        assertEquals(SpeedBand.FLYING, RideClassification.of(ride(34.5, flat)).speed)
    }

    @Test
    fun `the existing fast flag is unchanged`() {
        assertEquals(true, RideClassification.of(ride(30.0, flat)).fast)
        assertEquals(false, RideClassification.of(ride(29.0, flat)).fast)
    }
}
