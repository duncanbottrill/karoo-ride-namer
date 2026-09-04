package com.duncanbottrill.ridenamer.name

import kotlin.random.Random

/**
 * Names built from pure nonsense: one to three unrelated words pulled out of
 * [RandomWordBanks], with no reference to the ride at all.
 *
 *     1 -> "Marmalade"    2 -> "Cobra Lantern"    3 -> "Velvet Marmalade Thunder"
 *
 * Words within one name are always distinct, so you never get "Otter Otter".
 * Deterministic for a given seed, like the other generators.
 */
object RandomNameGenerator {

    fun generate(wordCount: Int = WordCount.DEFAULT, seed: Long? = null): String {
        val rng = if (seed != null) Random(seed) else Random.Default
        val words = wordCount.coerceIn(WordCount.MIN, WordCount.MAX)
        // shuffled().take(n) rather than n independent draws, so the words can't repeat.
        return RandomWordBanks.words.shuffled(rng).take(words).joinToString(" ")
    }
}
