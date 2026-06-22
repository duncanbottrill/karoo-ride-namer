package com.duncanbottrill.ridenamer.strava

import android.util.Log
import com.duncanbottrill.ridenamer.data.RideNamerStore
import com.duncanbottrill.ridenamer.data.StravaCredentials
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URLEncoder
import java.time.Instant
import kotlin.math.abs

/**
 * Talks to the Strava v3 API: refreshes tokens, finds the activity matching a finished
 * ride, and renames it. Strava activities are uploaded by the Karoo a little after the
 * ride ends, so renames are queued and retried until the activity appears.
 */
class StravaClient(
    private val store: RideNamerStore,
    private val engine: HttpEngine,
) {
    companion object {
        private const val TAG = "RideNamer/Strava"
        const val REDIRECT_URI = "ridenamer://strava-callback"
        const val OAUTH_BASE = "https://www.strava.com/oauth"
        const val API_BASE = "https://www.strava.com/api/v3"
        // Tolerance when matching a Strava activity to our ride by start time.
        private const val START_MATCH_TOLERANCE_SEC = 600L
    }

    private val json = Json { ignoreUnknownKeys = true }

    fun authorizeUrl(clientId: String): String =
        "$OAUTH_BASE/authorize?client_id=$clientId" +
            "&response_type=code" +
            "&redirect_uri=${enc(REDIRECT_URI)}" +
            "&approval_prompt=auto" +
            "&scope=activity:read,activity:write"

    /** Exchanges the OAuth code for tokens and stores the refresh token. */
    suspend fun exchangeCode(code: String): Boolean {
        val creds = store.stravaCredentials.first()
        if (!creds.isConfigured) return false
        val body = form(
            "client_id" to creds.clientId,
            "client_secret" to creds.clientSecret,
            "code" to code,
            "grant_type" to "authorization_code",
        )
        val res = engine.request("POST", "$OAUTH_BASE/token", formHeaders(), body)
        if (!res.isSuccess || res.body == null) {
            Log.w(TAG, "Code exchange failed: ${res.statusCode} ${res.body}")
            return false
        }
        val token = json.decodeFromString<TokenResponse>(res.body)
        store.setStravaCredentials(
            creds.copy(
                refreshToken = token.refresh_token ?: creds.refreshToken,
                accessToken = token.access_token ?: "",
                accessTokenExpiresAtSec = token.expires_at ?: 0L,
            ),
        )
        return true
    }

    /** Returns a valid access token, refreshing if needed, or null if not connected. */
    private suspend fun validAccessToken(): String? {
        val creds = store.stravaCredentials.first()
        if (!creds.isConnected) return null
        val now = Instant.now().epochSecond
        if (creds.accessToken.isNotBlank() && creds.accessTokenExpiresAtSec - 60 > now) {
            return creds.accessToken
        }
        val body = form(
            "client_id" to creds.clientId,
            "client_secret" to creds.clientSecret,
            "refresh_token" to creds.refreshToken,
            "grant_type" to "refresh_token",
        )
        val res = engine.request("POST", "$OAUTH_BASE/token", formHeaders(), body)
        if (!res.isSuccess || res.body == null) {
            Log.w(TAG, "Token refresh failed: ${res.statusCode} ${res.body}")
            return null
        }
        val token = json.decodeFromString<TokenResponse>(res.body)
        store.setStravaCredentials(
            creds.copy(
                refreshToken = token.refresh_token ?: creds.refreshToken,
                accessToken = token.access_token ?: "",
                accessTokenExpiresAtSec = token.expires_at ?: 0L,
            ),
        )
        return token.access_token
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
     * Processes the queued renames. Returns the renames that are still pending
     * (activity not found yet, or transient failure) so the caller can re-store them.
     */
    suspend fun processPending(): ProcessResult {
        val pending = store.pendingRenames.first()
        if (pending.isEmpty()) return ProcessResult(emptyList(), emptyList())
        val token = validAccessToken() ?: return ProcessResult(pending, emptyList())

        val stillPending = mutableListOf<com.duncanbottrill.ridenamer.data.PendingRename>()
        val applied = mutableListOf<com.duncanbottrill.ridenamer.data.PendingRename>()
        for (job in pending) {
            val activityId = runCatching { findActivityId(job.rideStartEpochMs, token) }.getOrNull()
            if (activityId == null) {
                // Not uploaded yet (or transient) — keep trying unless it's gone stale (>2 days).
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
        val stillPending: List<com.duncanbottrill.ridenamer.data.PendingRename>,
        val applied: List<com.duncanbottrill.ridenamer.data.PendingRename>,
    )

    // --- request helpers ---

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
    private fun form(vararg pairs: Pair<String, String>) =
        pairs.joinToString("&") { "${enc(it.first)}=${enc(it.second)}" }.toByteArray(Charsets.UTF_8)

    private fun formHeaders() = mapOf("Content-Type" to "application/x-www-form-urlencoded")
    private fun bearer(token: String) = mapOf("Authorization" to "Bearer $token")

    // --- Strava DTOs ---

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
