package com.duncanbottrill.ridenamer.name

import com.duncanbottrill.ridenamer.model.RideStats
import kotlin.random.Random

/**
 * Terse names built from three single words — a weather colour, a time of day and a pace:
 *
 *     1 -> "Slate"    2 -> "Slate Crawl"    3 -> "Slate Midnight Crawl"
 *
 * The colour always survives, so even a one-word name carries the ride's most distinctive
 * signal; the time word is the first thing dropped. Deterministic for a given seed, like
 * the other generators.
 */
object MinimalNameGenerator {

    const val MIN_WORDS = 1
    const val MAX_WORDS = 3
    const val DEFAULT_WORDS = 3

    fun generate(stats: RideStats, wordCount: Int = DEFAULT_WORDS, seed: Long? = null): String {
        val rng = if (seed != null) Random(seed) else Random.Default
        val words = wordCount.coerceIn(MIN_WORDS, MAX_WORDS)
        val c = RideClassification.of(stats)

        // all three words are drawn unconditionally, so a given seed produces
        // the same colour regardless of the chosen word count.
        val timeWord = MinimalWordBanks.times.getValue(c.time).random(rng)
        // No weather (or a WMO code we don't recognise) falls back to the sky's colour at
        // that hour, so the user's chosen word count is honoured either way.
        val colourBank = c.condition?.let { MinimalWordBanks.colours(it, c.temp) }
            ?: MinimalWordBanks.skyColours.getValue(c.time)
        val colourWord = pickDistinct(colourBank, avoid = timeWord, rng = rng)
        val speedWord = MinimalWordBanks.speeds.getValue(c.speed).random(rng)

        return when (words) {
            1 -> colourWord
            2 -> "$colourWord $speedWord"
            else -> "$colourWord $timeWord $speedWord"
        }
    }

    /** Stops a name reading "Midnight Midnight Crawl" if a bank ever overlaps the time words. */
    private fun pickDistinct(bank: List<String>, avoid: String, rng: Random): String {
        val options = bank.filter { !it.equals(avoid, ignoreCase = true) }
        return if (options.isEmpty()) bank.first() else options.random(rng)
    }
}
