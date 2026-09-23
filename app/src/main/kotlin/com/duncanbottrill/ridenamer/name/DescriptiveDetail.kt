package com.duncanbottrill.ridenamer.name

/**
 * How much the Descriptive style says. [FULL] is the original behaviour — varied sentences
 * and a stat label, including place and time of day. The other three are compact forms
 * built from distance and terrain, with an optional third word.
 */
enum class DescriptiveDetail(val label: String) {
    FULL("Full sentence"),
    DISTANCE_TERRAIN("Distance + terrain"),
    WITH_EFFORT("Distance + terrain + effort"),
    WITH_WEATHER("Distance + terrain + weather"),
    ;

    companion object {
        val DEFAULT = FULL
        fun fromName(name: String?): DescriptiveDetail = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
