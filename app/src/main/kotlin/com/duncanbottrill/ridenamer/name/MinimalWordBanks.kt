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
 *  3. Colour words are plain, everyday colour names — the ones a person names when asked
 *     to name a colour. "Putty" and "Verdigris" are colours in a paint catalogue, but in a
 *     ride name they read as a material rather than a colour.
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
            TempBand.FREEZING, TempBand.COLD -> listOf("Blue", "White", "Silver")
            TempBand.WARM, TempBand.SCORCHING -> listOf("Orange", "Red", "Gold")
            TempBand.MILD, null -> listOf("Yellow", "Gold", "Blue")
        }
        WeatherCondition.CLOUDY -> when (temp) {
            TempBand.FREEZING, TempBand.COLD -> listOf("Grey", "Silver", "Blue")
            else -> listOf("Grey", "White", "Brown")
        }
        WeatherCondition.FOG -> listOf("Grey", "White", "Silver")
        WeatherCondition.DRIZZLE -> listOf("Green", "Grey", "Brown")
        WeatherCondition.RAIN -> listOf("Grey", "Blue", "Black")
        WeatherCondition.SNOW -> listOf("White", "Silver", "Blue")
        WeatherCondition.THUNDER -> listOf("Black", "Purple", "Grey")
        WeatherCondition.UNKNOWN -> null
    }

    /**
     * Colours for when the weather fetch failed or returned a code we don't recognise —
     * the colour the sky plausibly was at that hour. Deliberately shares no words with
     * [times].
     */
    val skyColours: Map<TimeBand, List<String>> = mapOf(
        TimeBand.DAWN to listOf("Pink", "Orange", "Red"),
        TimeBand.MORNING to listOf("Blue", "Green", "White"),
        TimeBand.MIDDAY to listOf("Blue", "Gold", "White"),
        TimeBand.AFTERNOON to listOf("Gold", "Yellow", "Orange"),
        TimeBand.EVENING to listOf("Orange", "Red", "Purple"),
        TimeBand.NIGHT to listOf("Black", "Blue", "Purple"),
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
