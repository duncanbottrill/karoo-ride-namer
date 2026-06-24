package com.duncanbottrill.ridenamer.strava

import android.util.Log
import com.duncanbottrill.ridenamer.data.PendingRename
import com.duncanbottrill.ridenamer.data.RideNamerStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URLEncoder
import java.time.Instant
import kotlin.math.abs

/**
 * Talks to Strava, but routes the secret-requiring token exchange/refresh through our
 * backend (see `/backend`) so the client secret never lives in the app. Activity listing
 * and renaming go straight to Strava using the access token.
 */
class StravaClient(
    private val store: RideNamerStore,
    private val engine: HttpEngine,
) {
    companion object {
        private const val TAG = "RideNamer/Strava"
        const val REDIRECT_URI = "ridenamer://strava-callback"
        private const val API_BASE = "https://www.strava.com/api/v3"
        private const val START_MATCH_TOLERANCE_SEC = 600L
    }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    /** Fetches the public client id from the backend and builds the Strava authorize URL. */
    suspend fun authorizeUrl(): String? {
        val res = engine.request("GET", "$STRAVA_BACKEND_URL/api/config", emptyMap(), null)
        if (!res.isSuccess || res.body == null) {
            Log.w(TAG, "Config fetch failed: ${res.statusCode} ${res.body}")
            return null
        }
        val clientId = runCatching { json.decodeFromString<ConfigResponse>(res.body) }.getOrNull()?.clientId
        if (clientId.isNullOrBlank()) return null
        return "https://www.strava.com/oauth/authorize?client_id=$clientId" +
            "&response_type=code" +
            "&redirect_uri=${enc(REDIRECT_URI)}" +
            "&approval_prompt=auto" +
            "&scope=activity:read,activity:write"
    }

    /** Exchanges the OAuth code for tokens via the backend (which holds the secret). */
    suspend fun exchangeCode(code: String): Boolean {
        val token = postToken(TokenRequest(grant_type = "authorization_code", code = code)) ?: return false
        saveTokens(token)
        return true
    }

    /** Returns a valid access token, refreshing via the backend if needed; null if not connected. */
    private suspend fun validAccessToken(): String? {
        val creds = store.stravaCredentials.first()
        if (!creds.isConnected) return null
        val now = Instant.now().epochSecond
        if (creds.accessToken.isNotBlank() && creds.accessTokenExpiresAtSec - 60 > now) {
            return creds.accessToken
        }
        val token = postToken(TokenRequest(grant_type = "refresh_token", refresh_token = creds.refreshToken))
            ?: return null
        saveTokens(token)
        return token.access_token
    }

    private suspend fun postToken(request: TokenRequest): TokenResponse? {
        val body = json.encodeToString(request).toByteArray(Charsets.UTF_8)
        val res = engine.request(
            "POST",
            "$STRAVA_BACKEND_URL/api/token",
            mapOf("Content-Type" to "application/json"),
            body,
        )
        if (!res.isSuccess || res.body == null) {
            Log.w(TAG, "Token endpoint failed: ${res.statusCode} ${res.body}")
            return null
        }
        return runCatching { json.decodeFromString<TokenResponse>(res.body) }.getOrNull()
    }

    private suspend fun saveTokens(token: TokenResponse) {
        val creds = store.stravaCredentials.first()
        store.setStravaCredentials(
            creds.copy(
                refreshToken = token.refresh_token ?: creds.refreshToken,
                accessToken = token.access_token ?: "",
                accessTokenExpiresAtSec = token.expires_at ?: 0L,
            ),
        )
    }

    /** Finds the cycling activity whose start time best matches [rideStartEpochMs]. */
    private suspend fun findActivityId(rideStartEpochMs: Long, token: String): Long? {
        val startSec = rideStartEpochMs / 1000
        val after = startSec - START_MATCH_TOLERANCE_SEC
        val before = startSec + START_MATCH_TOLERANCE_SEC
        val url = "$API_BASE/athlete/activities?after=$after&before=$before&per_page=30"
        val res = engine.request("GET", url, bearer(token), null)
        if (!res.isSuccess || res.body == null) {
            Log.w(TAG, "List activities failed: ${res.statusCode} ${res.body}")
            return null
        }
        val activities = json.decodeFromString<List<SummaryActivity>>(res.body)
        return activities
            .filter { it.isRide() }
            .minByOrNull { abs(it.startEpochSec() - startSec) }
            ?.id
    }

    private suspend fun renameActivity(id: Long, name: String, token: String): Boolean {
        val body = form("name" to name)
        val res = engine.request("PUT", "$API_BASE/activities/$id", bearer(token) + formHeaders(), body)
        if (!res.isSuccess) Log.w(TAG, "Rename failed: ${res.statusCode} ${res.body}")
        return res.isSuccess
    }

    /**
     * Processes the queued renames. Returns the renames still pending (activity not found
     * yet, or transient failure) so the caller can re-store them.
     */
    suspend fun processPending(): ProcessResult {
        val pending = store.pendingRenames.first()
        if (pending.isEmpty()) return ProcessResult(emptyList(), emptyList())
        val token = validAccessToken() ?: return ProcessResult(pending, emptyList())

        val stillPending = mutableListOf<PendingRename>()
        val applied = mutableListOf<PendingRename>()
        for (job in pending) {
            val activityId = runCatching { findActivityId(job.rideStartEpochMs, token) }.getOrNull()
            if (activityId == null) {
                val ageMs = System.currentTimeMillis() - job.rideEndEpochMs
                if (ageMs < 2 * 24 * 60 * 60 * 1000L) stillPending += job.copy(attempts = job.attempts + 1)
                continue
            }
            val ok = runCatching { renameActivity(activityId, job.name, token) }.getOrDefault(false)
            if (ok) applied += job else stillPending += job.copy(attempts = job.attempts + 1)
        }
        store.setPendingRenames(stillPending)
        return ProcessResult(stillPending, applied)
    }

    data class ProcessResult(
        val stillPending: List<PendingRename>,
        val applied: List<PendingRename>,
    )

    // --- request helpers ---

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
    private fun form(vararg pairs: Pair<String, String>) =
        pairs.joinToString("&") { "${enc(it.first)}=${enc(it.second)}" }.toByteArray(Charsets.UTF_8)

    private fun formHeaders() = mapOf("Content-Type" to "application/x-www-form-urlencoded")
    private fun bearer(token: String) = mapOf("Authorization" to "Bearer $token")

    // --- DTOs ---

    @Serializable
    private data class ConfigResponse(val clientId: String = "")

    @Serializable
    private data class TokenRequest(
        val grant_type: String,
        val code: String? = null,
        val refresh_token: String? = null,
    )

    @Serializable
    private data class TokenResponse(
        val access_token: String? = null,
        val refresh_token: String? = null,
        val expires_at: Long? = null,
    )

    @Serializable
    private data class SummaryActivity(
        val id: Long,
        val name: String? = null,
        val type: String? = null,
        val sport_type: String? = null,
        val start_date: String? = null,
    ) {
        fun isRide(): Boolean {
            val t = (sport_type ?: type ?: "").lowercase()
            return t.contains("ride") || t.contains("cycl")
        }

        fun startEpochSec(): Long =
            start_date?.let { runCatching { Instant.parse(it).epochSecond }.getOrNull() } ?: 0L
    }
}
