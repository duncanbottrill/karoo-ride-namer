package com.duncanbottrill.ridenamer.name

import com.duncanbottrill.ridenamer.model.WeatherCondition
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MinimalWordBanksTest {

    /** Every colour bank reachable through the public API, including the null-temp case. */
    private fun allColourBanks(): List<List<String>> {
        val temps: List<TempBand?> = TempBand.entries + listOf(null)
        val weather = WeatherCondition.entries
            .flatMap { condition -> temps.map { temp -> MinimalWordBanks.colours(condition, temp) } }
            .filterNotNull()
        return weather + MinimalWordBanks.skyColours.values
    }

    private fun allBanks(): List<List<String>> =
        allColourBanks() + MinimalWordBanks.times.values + MinimalWordBanks.speeds.values

    @Test
    fun `every word is a single token`() {
        allBanks().flatten().forEach { word ->
            assertTrue("'$word' must not contain whitespace", word.none { it.isWhitespace() })
            assertTrue("'$word' must not be blank", word.isNotBlank())
        }
    }

    @Test
    fun `no bank is empty`() {
        // A generator picking at random from an empty list would throw mid-ride.
        allBanks().forEach { assertTrue("bank must not be empty", it.isNotEmpty()) }
    }

    @Test
    fun `every band has a bank`() {
        TimeBand.entries.forEach {
            assertNotNull("no sky colours for $it", MinimalWordBanks.skyColours[it])
            assertNotNull("no time words for $it", MinimalWordBanks.times[it])
        }
        SpeedBand.entries.forEach { assertNotNull("no speed words for $it", MinimalWordBanks.speeds[it]) }
    }

    @Test
    fun `unknown weather has no colour bank`() {
        TempBand.entries.forEach {
            assertNull(MinimalWordBanks.colours(WeatherCondition.UNKNOWN, it))
        }
        assertNull(MinimalWordBanks.colours(WeatherCondition.UNKNOWN, null))
    }

    @Test
    fun `known weather always has a colour bank`() {
        val known = WeatherCondition.entries - WeatherCondition.UNKNOWN
        val temps: List<TempBand?> = TempBand.entries + listOf(null)
        known.forEach { condition ->
            temps.forEach { temp ->
                assertNotNull("$condition / $temp has no colours", MinimalWordBanks.colours(condition, temp))
            }
        }
    }

    @Test
    fun `colour words never collide with time words`() {
        // Guards against "Midnight Midnight Crawl". The generator also re-draws, but the
        // banks should not rely on that.
        val colours = allColourBanks().flatten().map { it.lowercase() }.toSet()
        val times = MinimalWordBanks.times.values.flatten().map { it.lowercase() }.toSet()
        val overlap = colours intersect times
        assertTrue("colour and time banks overlap: $overlap", overlap.isEmpty())
    }
}
