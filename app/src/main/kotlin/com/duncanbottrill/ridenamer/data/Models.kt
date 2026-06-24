package com.duncanbottrill.ridenamer.data

import com.duncanbottrill.ridenamer.model.RideStats
import kotlinx.serialization.Serializable

/** A name we generated for a finished ride, kept for the history screen. */
@Serializable
data class HistoryEntry(
    val name: String,
    val stats: RideStats,
    val generatedAtMs: Long,
    val stravaStatus: StravaStatus = StravaStatus.NOT_CONFIGURED,
)

enum class StravaStatus { NOT_CONFIGURED, PENDING, APPLIED, FAILED }

/** A queued "rename this Strava activity once it shows up" job. */
@Serializable
data class PendingRename(
    val name: String,
    val rideStartEpochMs: Long,
    val rideEndEpochMs: Long,
    val attempts: Int = 0,
)

/**
 * Strava tokens stored on-device after authorization. The client id/secret are NOT here —
 * they live only in the backend (see `/backend`), so the app never holds the secret.
 */
@Serializable
data class StravaCredentials(
    val refreshToken: String = "",
    val accessToken: String = "",
    val accessTokenExpiresAtSec: Long = 0L,
) {
    val isConnected: Boolean get() = refreshToken.isNotBlank()
}
