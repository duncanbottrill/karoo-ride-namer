package com.duncanbottrill.ridenamer.model

import kotlinx.serialization.Serializable

/** Weather captured once, near the start of the ride (cached so we don't need signal later). */
@Serializable
data class WeatherSnapshot(
    val temperatureC: Double,
    val windSpeedKmh: Double,
    /** WMO weather interpretation code as returned by Open-Meteo. */
    val weatherCode: Int,
) {
    val condition: WeatherCondition get() = WeatherCondition.fromWmo(weatherCode)
}

enum class WeatherCondition {
    CLEAR, CLOUDY, FOG, DRIZZLE, RAIN, SNOW, THUNDER, UNKNOWN;

    companion object {
        // https://open-meteo.com/en/docs — WMO weather interpretation codes
        fun fromWmo(code: Int): WeatherCondition = when (code) {
            0 -> CLEAR
            1, 2, 3 -> CLOUDY
            45, 48 -> FOG
            51, 53, 55, 56, 57 -> DRIZZLE
            61, 63, 65, 66, 67, 80, 81, 82 -> RAIN
            71, 73, 75, 77, 85, 86 -> SNOW
            95, 96, 99 -> THUNDER
            else -> UNKNOWN
        }
    }
}
