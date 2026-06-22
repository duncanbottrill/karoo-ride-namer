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

/** Strava OAuth credentials + tokens. clientId/clientSecret come from the user's Strava API app. */
@Serializable
data class StravaCredentials(
    val clientId: String = "",
    val clientSecret: String = "",
    val refreshToken: String = "",
    val accessToken: String = "",
    val accessTokenExpiresAtSec: Long = 0L,
) {
    val isConfigured: Boolean get() = clientId.isNotBlank() && clientSecret.isNotBlank()
    val isConnected: Boolean get() = isConfigured && refreshToken.isNotBlank()
}
