package com.duncanbottrill.ridenamer.strava

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.duncanbottrill.ridenamer.data.RideNamerStore
import com.duncanbottrill.ridenamer.data.StravaStatus
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Applies queued Strava renames in the background, surviving the app/process being killed.
 * The Karoo uploads a finished ride to Strava a few minutes after it ends, so this retries
 * (with backoff, only when there's network) until the activity shows up and is renamed.
 */
class StravaRenameWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val store = RideNamerStore(applicationContext)
        if (!store.stravaCredentials.first().isConnected) return Result.success()
        if (store.pendingRenames.first().isEmpty()) return Result.success()

        val result = runCatching {
            StravaClient(store, directHttpEngine()).processPending()
        }.getOrElse { return Result.retry() }

        result.applied.forEach { store.updateHistoryStatus(it.rideEndEpochMs, StravaStatus.APPLIED) }

        // If anything is still pending, the activity probably hasn't uploaded yet — retry.
        return if (result.stillPending.isEmpty()) Result.success() else Result.retry()
    }

    companion object {
        private const val WORK_NAME = "strava-rename"

        /** Enqueue a (re)try of all pending renames. Safe to call repeatedly. */
        fun schedule(context: Context) {
            val request = OneTimeWorkRequestBuilder<StravaRenameWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
