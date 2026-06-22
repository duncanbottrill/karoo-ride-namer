package com.duncanbottrill.ridenamer.karoo

import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.models.HttpResponseState
import io.hammerhead.karooext.models.OnHttpResponse
import io.hammerhead.karooext.models.OnLocationChanged
import io.hammerhead.karooext.models.OnStreamState
import io.hammerhead.karooext.models.RideState
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UserProfile
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.timeout
import kotlin.time.Duration.Companion.seconds

fun KarooSystemService.streamRideState(): Flow<RideState> = callbackFlow {
    val id = addConsumer { event: RideState -> trySendBlocking(event) }
    awaitClose { removeConsumer(id) }
}

fun KarooSystemService.streamDataFlow(dataTypeId: String): Flow<StreamState> = callbackFlow {
    val id = addConsumer(OnStreamState.StartStreaming(dataTypeId)) { event: OnStreamState ->
        trySendBlocking(event.state)
    }
    awaitClose { removeConsumer(id) }
}

fun KarooSystemService.streamLocation(): Flow<OnLocationChanged> = callbackFlow {
    val id = addConsumer { event: OnLocationChanged -> trySendBlocking(event) }
    awaitClose { removeConsumer(id) }
}

fun KarooSystemService.streamUserProfile(): Flow<UserProfile> = callbackFlow {
    val id = addConsumer { profile: UserProfile -> trySendBlocking(profile) }
    awaitClose { removeConsumer(id) }
}

/**
 * Fires a single HTTP request through the Karoo system's network stack and suspends
 * until it completes (or times out). Used for both weather and Strava calls so they
 * ride on whatever connectivity the head unit has.
 */
@OptIn(kotlinx.coroutines.FlowPreview::class)
suspend fun KarooSystemService.httpRequest(
    method: String,
    url: String,
    headers: Map<String, String> = emptyMap(),
    body: ByteArray? = null,
    waitForConnection: Boolean = false,
    timeoutSeconds: Long = 30,
): HttpResponseState.Complete = callbackFlow {
    val id = addConsumer(
        OnHttpResponse.MakeHttpRequest(
            method = method,
            url = url,
            headers = headers,
            body = body,
            waitForConnection = waitForConnection,
        ),
        onError = { err -> close(RuntimeException("HTTP error: $err")) },
        onEvent = { event: OnHttpResponse ->
            (event.state as? HttpResponseState.Complete)?.let {
                trySendBlocking(it)
                close()
            }
        },
    )
    awaitClose { removeConsumer(id) }
}.timeout(timeoutSeconds.seconds)
    .catch { e ->
        if (e is kotlinx.coroutines.TimeoutCancellationException) {
            emit(HttpResponseState.Complete(408, emptyMap(), null, "Timeout"))
        } else {
            throw e
        }
    }
    .single()
