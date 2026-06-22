package com.duncanbottrill.ridenamer.weather

import android.util.Log
import com.duncanbottrill.ridenamer.karoo.httpRequest
import com.duncanbottrill.ridenamer.model.WeatherSnapshot
import io.hammerhead.karooext.KarooSystemService
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Locale

/**
 * Fetches current conditions from Open-Meteo (free, no API key). Called once near the
 * start of a ride and cached, so a lost signal mid-ride doesn't matter.
 */
class WeatherClient(private val karoo: KarooSystemService) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetch(lat: Double, lon: Double): WeatherSnapshot? {
        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=${fmt(lat)}&longitude=${fmt(lon)}" +
            "&current=temperature_2m,weather_code,wind_speed_10m" +
            "&wind_speed_unit=kmh&timeformat=unixtime"
        return runCatching {
            val res = karoo.httpRequest("GET", url, mapOf("User-Agent" to "karoo-ride-namer"))
            if (res.statusCode !in 200..299 || res.body == null) {
                Log.w(TAG, "Weather request failed: ${res.statusCode}")
                return null
            }
            val parsed = json.decodeFromString<Response>(res.body!!.toString(Charsets.UTF_8))
            WeatherSnapshot(
                temperatureC = parsed.current.temperature_2m,
                windSpeedKmh = parsed.current.wind_speed_10m,
                weatherCode = parsed.current.weather_code,
            )
        }.onFailure { Log.w(TAG, "Weather fetch error", it) }.getOrNull()
    }

    private fun fmt(v: Double) = String.format(Locale.US, "%.4f", v)

    @Serializable
    private data class Response(val current: Current)

    @Serializable
    private data class Current(
        val temperature_2m: Double,
        val weather_code: Int,
        val wind_speed_10m: Double,
    )

    companion object {
        private const val TAG = "RideNamer/Weather"
    }
}
