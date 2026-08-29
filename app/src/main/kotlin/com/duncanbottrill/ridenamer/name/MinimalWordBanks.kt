package com.duncanbottrill.ridenamer.name

import com.duncanbottrill.ridenamer.model.WeatherCondition

/**
 * Word lists for the Minimal style: one colour, one time-of-day word, one speed word.
 *
 * Three rules, all enforced by MinimalWordBanksTest, so add new words freely and let the
 * test keep you honest:
 *
 *  1. Every entry is a single token with no whitespace — that is what makes the user's
 *     "1, 2 or 3 words" choice literally true.
 *  2. No word appears in two different slots' banks, or a name could read
 *     "Midnight Midnight Crawl".
 *  3. Colour words are actual colour names. "Storm" and "Shroud" are weather, not colours,
 *     and reading one in the colour slot just looks like a second weather word.
 */
object MinimalWordBanks {

    /**
     * Colours for a known sky. Temperature only shifts the dry conditions, where it is the
     * dominant signal; when it is raining or snowing the condition speaks for itself.
     *
     * Returns null for [WeatherCondition.UNKNOWN] — the caller falls back to [skyColours].
     */
    fun colours(condition: WeatherCondition, temp: TempBand?): List<String>? = when (condition) {
        WeatherCondition.CLEAR -> when (temp) {
            TempBand.FREEZING, TempBand.COLD -> listOf("Frost", "Pewter", "Silver", "Ivory")
            TempBand.WARM, TempBand.SCORCHING -> listOf("Amber", "Ochre", "Brass", "Ember", "Terracotta")
            TempBand.MILD, null -> listOf("Gold", "Wheat", "Lemon", "Straw")
        }
        WeatherCondition.CLOUDY -> when (temp) {
            TempBand.FREEZING, TempBand.COLD -> listOf("Zinc", "Steel", "Dove")
            else -> listOf("Pewter", "Putty", "Oyster", "Chalk")
        }
        WeatherCondition.FOG -> listOf("Ash", "Smoke", "Milk", "Pearl")
        WeatherCondition.DRIZZLE -> listOf("Drab", "Moss", "Sage", "Verdigris")
        WeatherCondition.RAIN -> listOf("Slate", "Gunmetal", "Graphite", "Lead")
        WeatherCondition.SNOW -> listOf("Bone", "Alabaster", "Ice", "Linen", "Porcelain")
        WeatherCondition.THUNDER -> listOf("Ink", "Bruise", "Charcoal", "Sable")
        WeatherCondition.UNKNOWN -> null
    }

    /**
     * Colours for when the weather fetch failed or returned a code we don't recognise —
     * the colour the sky plausibly was at that hour. Deliberately shares no words with
     * [times].
     */
    val skyColours: Map<TimeBand, List<String>> = mapOf(
        TimeBand.DAWN to listOf("Rose", "Blush", "Coral", "Peach"),
        TimeBand.MORNING to listOf("Cyan", "Periwinkle", "Mint", "Eggshell"),
        TimeBand.MIDDAY to listOf("Azure", "Cobalt", "Cerulean"),
        TimeBand.AFTERNOON to listOf("Gold", "Wheat", "Sand", "Honey"),
        TimeBand.EVENING to listOf("Umber", "Rust", "Copper", "Sienna"),
        TimeBand.NIGHT to listOf("Indigo", "Obsidian", "Onyx", "Navy"),
    )

    val times: Map<TimeBand, List<String>> = mapOf(
        TimeBand.DAWN to listOf("Dawn", "Daybreak", "Sunrise", "Aurora"),
        TimeBand.MORNING to listOf("Morning", "Matins", "Forenoon", "Sunup"),
        TimeBand.MIDDAY to listOf("Noon", "Midday", "Zenith", "Meridian"),
        TimeBand.AFTERNOON to listOf("Afternoon", "Nones", "Waning"),
        TimeBand.EVENING to listOf("Evening", "Dusk", "Vespers", "Sundown", "Gloaming"),
        TimeBand.NIGHT to listOf("Midnight", "Nocturne", "Nightfall", "Witching"),
    )

    val speeds: Map<SpeedBand, List<String>> = mapOf(
        SpeedBand.CRAWL to listOf("Crawl", "Trudge", "Plod", "Slog", "Dawdle"),
        SpeedBand.AMBLE to listOf("Amble", "Pootle", "Saunter", "Drift"),
        SpeedBand.CRUISE to listOf("Cruise", "Tempo", "Steady", "Glide", "Rhythm"),
        SpeedBand.SWIFT to listOf("Sprint", "Surge", "Charge", "Hustle", "Chase"),
        SpeedBand.FLYING to listOf("Blitz", "Bolt", "Rocket", "Flight", "Warp"),
    )
}
