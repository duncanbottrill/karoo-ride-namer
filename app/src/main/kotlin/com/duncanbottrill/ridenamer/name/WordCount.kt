package com.duncanbottrill.ridenamer.name

/**
 * How many words the user may ask for from the styles that let them choose.
 * Shared by [MinimalNameGenerator] and [RandomNameGenerator] so the bounds and the
 * default can only ever be changed in one place.
 */
object WordCount {
    const val MIN = 1
    const val MAX = 3
    const val DEFAULT = 3
}
