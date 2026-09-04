package com.duncanbottrill.ridenamer.name

import com.duncanbottrill.ridenamer.model.WeatherCondition
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MinimalWordBanksTest {

    /**
     * The colour slot may only use these plain, everyday colour names. Adding a colour to a
     * bank means adding it here too — deliberately annoying, so the slot keeps reading as a
     * colour and not as a paint-catalogue shade.
     */
    private val allowedColours = setOf(
        "black", "blue", "brown", "gold", "green", "grey",
        "orange", "pink", "purple", "red", "silver", "white", "yellow",
    )

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
    fun `no word is shared between two slots`() {
        // Guards against "Midnight Midnight Crawl" and "Storm Storm". The generator also
        // re-draws the colour, but only against the time word — the banks must not rely
        // on that for the other two pairs.
        val colours = lowercased(allColourBanks())
        val times = lowercased(MinimalWordBanks.times.values)
        val speeds = lowercased(MinimalWordBanks.speeds.values)
        assertTrue("colour/time overlap: ${colours intersect times}", (colours intersect times).isEmpty())
        assertTrue("colour/speed overlap: ${colours intersect speeds}", (colours intersect speeds).isEmpty())
        assertTrue("time/speed overlap: ${times intersect speeds}", (times intersect speeds).isEmpty())
    }

    @Test
    fun `every colour word is actually a colour`() {
        // The spec calls this slot "a colour relating to the weather", and it has drifted
        // twice: first to weather nouns ("Storm", "Shroud"), then to shades nobody would
        // name as a colour ("Putty", "Oyster"). The bar is now plain everyday colours.
        val used = lowercased(allColourBanks())
        val notColours = used - allowedColours
        assertTrue("not colour names: $notColours", notColours.isEmpty())
    }

    @Test
    fun `the colour allow-list has no dead entries`() {
        // Keeps the allow-list honest in the other direction: an entry left behind after a
        // word is removed from a bank would quietly widen what the test permits.
        val unused = allowedColours - lowercased(allColourBanks())
        assertTrue("allow-list entries used by no bank: $unused", unused.isEmpty())
    }

    private fun lowercased(banks: Collection<List<String>>): Set<String> =
        banks.flatten().map { it.lowercase() }.toSet()
}
