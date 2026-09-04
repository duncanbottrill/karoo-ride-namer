package com.duncanbottrill.ridenamer.name

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RandomNameGeneratorTest {

    private fun words(name: String) = name.split(" ")

    @Test
    fun `word count is honoured exactly`() {
        assertEquals(1, words(RandomNameGenerator.generate(1, seed = 1L)).size)
        assertEquals(2, words(RandomNameGenerator.generate(2, seed = 1L)).size)
        assertEquals(3, words(RandomNameGenerator.generate(3, seed = 1L)).size)
    }

    @Test
    fun `the default word count is three`() {
        assertEquals(3, words(RandomNameGenerator.generate()).size)
    }

    @Test
    fun `out of range word counts are coerced, not thrown`() {
        assertEquals(1, words(RandomNameGenerator.generate(0, seed = 1L)).size)
        assertEquals(1, words(RandomNameGenerator.generate(-5, seed = 1L)).size)
        assertEquals(3, words(RandomNameGenerator.generate(4, seed = 1L)).size)
        assertEquals(3, words(RandomNameGenerator.generate(99, seed = 1L)).size)
    }

    @Test
    fun `is deterministic per seed`() {
        assertEquals(
            RandomNameGenerator.generate(3, seed = 5L),
            RandomNameGenerator.generate(3, seed = 5L),
        )
    }

    @Test
    fun `different seeds vary the name`() {
        val names = (1L..40L).map { RandomNameGenerator.generate(3, seed = it) }.toSet()
        assertTrue("expected variety, got $names", names.size > 1)
    }

    @Test
    fun `words within one name are never repeated`() {
        (1L..200L).forEach { seed ->
            val name = words(RandomNameGenerator.generate(3, seed = seed))
            assertEquals("repeated word in ${name.joinToString(" ")}", name.size, name.toSet().size)
        }
    }

    @Test
    fun `every word comes from the bank`() {
        (1L..50L).forEach { seed ->
            words(RandomNameGenerator.generate(3, seed = seed)).forEach {
                assertTrue("'$it' is not in the bank", RandomWordBanks.words.contains(it))
            }
        }
    }

    @Test
    fun `never produces a blank name`() {
        (1L..30L).forEach { seed ->
            (1..3).forEach { count ->
                assertTrue(RandomNameGenerator.generate(count, seed = seed).isNotBlank())
            }
        }
    }
}
