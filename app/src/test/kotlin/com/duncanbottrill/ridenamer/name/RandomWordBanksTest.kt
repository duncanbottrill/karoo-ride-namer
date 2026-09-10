package com.duncanbottrill.ridenamer.name

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RandomWordBanksTest {

    @Test
    fun `every word is a single token`() {
        RandomWordBanks.words.forEach { word ->
            assertTrue("'$word' must not contain whitespace", word.none { it.isWhitespace() })
            assertTrue("'$word' must not be blank", word.isNotBlank())
        }
    }

    @Test
    fun `there are no duplicates`() {
        // A duplicate would weight one word more heavily and shrink the effective pool.
        val seen = RandomWordBanks.words.map { it.lowercase() }
        val dupes = seen.groupingBy { it }.eachCount().filter { it.value > 1 }.keys
        assertTrue("duplicated words: $dupes", dupes.isEmpty())
        assertEquals(seen.size, seen.toSet().size)
    }

    @Test
    fun `the pool is big enough to be worth calling random`() {
        // A one-word name draws from this list directly, so the pool size IS the number of
        // possible names and repeats arrive around its square root. At 121 words a one-word
        // name repeated every ~13 rides, which is what prompted this floor. 500 puts that
        // at ~27 rides; don't let the pool shrink back under it.
        assertTrue("pool is only ${RandomWordBanks.words.size} words", RandomWordBanks.words.size >= 500)
    }
}
