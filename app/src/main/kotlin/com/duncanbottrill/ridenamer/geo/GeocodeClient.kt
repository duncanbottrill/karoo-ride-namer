package com.duncanbottrill.ridenamer.geo

import android.util.Log
import com.duncanbottrill.ridenamer.karoo.httpRequest
import io.hammerhead.karooext.KarooSystemService
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Locale

/**
 * Reverse-geocodes the ride's start coordinates into a friendly place name using
 * BigDataCloud's client endpoint (free, no API key). Called once at ride start.
 */
class GeocodeClient(private val karoo: KarooSystemService) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun placeName(lat: Double, lon: Double): String? {
        val url = "https://api.bigdatacloud.net/data/reverse-geocode-client" +
            "?latitude=${fmt(lat)}&longitude=${fmt(lon)}&localityLanguage=en"
        return runCatching {
            val res = karoo.httpRequest("GET", url, mapOf("User-Agent" to "karoo-ride-namer"))
            if (res.statusCode !in 200..299 || res.body == null) {
                Log.w(TAG, "Geocode request failed: ${res.statusCode}")
                return null
            }
            val r = json.decodeFromString<Response>(res.body!!.toString(Charsets.UTF_8))
            // Prefer the most specific, recognisable label available.
            listOf(r.locality, r.city, r.principalSubdivision)
                .firstOrNull { !it.isNullOrBlank() }
                ?.trim()
        }.onFailure { Log.w(TAG, "Geocode error", it) }.getOrNull()
    }

    private fun fmt(v: Double) = String.format(Locale.US, "%.4f", v)

    @Serializable
    private data class Response(
        val locality: String? = null,
        val city: String? = null,
        val principalSubdivision: String? = null,
        val countryName: String? = null,
    )

    companion object {
        private const val TAG = "RideNamer/Geocode"
    }
}
