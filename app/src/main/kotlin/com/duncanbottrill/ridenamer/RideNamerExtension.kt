package com.duncanbottrill.ridenamer

import android.util.Log
import com.duncanbottrill.ridenamer.data.HistoryEntry
import com.duncanbottrill.ridenamer.data.PendingRename
import com.duncanbottrill.ridenamer.data.RideNamerStore
import com.duncanbottrill.ridenamer.data.StravaStatus
import com.duncanbottrill.ridenamer.geo.GeocodeClient
import com.duncanbottrill.ridenamer.karoo.streamDataFlow
import com.duncanbottrill.ridenamer.karoo.streamLocation
import com.duncanbottrill.ridenamer.karoo.streamRideState
import com.duncanbottrill.ridenamer.model.RideStats
import com.duncanbottrill.ridenamer.model.WeatherSnapshot
import com.duncanbottrill.ridenamer.name.RideNameGenerator
import com.duncanbottrill.ridenamer.strava.StravaClient
import com.duncanbottrill.ridenamer.strava.karooHttpEngine
import com.duncanbottrill.ridenamer.weather.WeatherClient
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.KarooExtension
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.RideState
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.SystemNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

/**
 * The always-on Karoo extension. It watches ride state, accumulates the ride's stats
 * while recording, grabs the weather once at the start, and — when the ride ends —
 * generates a name, shows a notification, and queues a Strava rename.
 */
class RideNamerExtension : KarooExtension("ridenamer", BuildConfig.VERSION_NAME) {

    private lateinit var karoo: KarooSystemService
    private lateinit var store: RideNamerStore
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val live = LiveRide()
    private var rideJob: Job? = null
    private var sessionActive = false

    override fun onCreate() {
        super.onCreate()
        store = RideNamerStore(applicationContext)
        karoo = KarooSystemService(applicationContext)
        karoo.connect { connected -> Log.i(TAG, "Karoo connected: $connected") }
        scope.launch { monitorRideState() }
        scope.launch { retryStravaLoop() }
    }

    override fun onDestroy() {
        rideJob?.cancel()
        scope.cancel()
        karoo.disconnect()
        super.onDestroy()
    }

    private suspend fun monitorRideState() {
        karoo.streamRideState().collect { state ->
            when (state) {
                is RideState.Recording -> if (!sessionActive) startSession()
                is RideState.Idle -> if (sessionActive) endSession()
                is RideState.Paused -> Unit // keep accumulating; ride isn't over
            }
        }
    }

    private fun startSession() {
        Log.i(TAG, "Ride started — collecting stats")
        sessionActive = true
        live.reset(System.currentTimeMillis())
        rideJob = scope.launch { collectStreams() }
        scope.launch { captureWeather() }
    }

    private suspend fun endSession() {
        Log.i(TAG, "Ride ended — generating name")
        sessionActive = false
        rideJob?.cancel()
        rideJob = null

        if (live.distanceM < 300.0) {
            Log.i(TAG, "Ride too short (${"%.2f".format(live.distanceM / 1000.0)}km) — skipping")
            return
        }

        // Resolve the place name from the ride's midpoint (needs a connection now; if
        // offline the name is simply generated without a place).
        live.midpoint()?.let { (lat, lng) ->
            GeocodeClient(karoo).placeName(lat, lng)?.let {
                live.placeName = it
                Log.i(TAG, "Midpoint place: $it")
            }
        }

        val stats = live.toStats(System.currentTimeMillis())
        val name = RideNameGenerator.generate(stats, seed = stats.endEpochMs)
        Log.i(TAG, "Generated name: $name")

        val connected = store.stravaCredentials.first().isConnected
        val status = if (connected) StravaStatus.PENDING else StravaStatus.NOT_CONFIGURED
        store.addHistory(HistoryEntry(name, stats, stats.endEpochMs, status))
        showNotification(name, stats)

        if (connected) {
            store.addPendingRename(PendingRename(name, stats.startEpochMs, stats.endEpochMs))
            processStrava()
        }
    }

    private suspend fun collectStreams() = coroutineScope {
        fun value(type: String) = karoo.streamDataFlow(type)
            .filterIsInstance<StreamState.Streaming>()
            .mapNotNull { it.dataPoint.singleValue }

        // Values arrive in metric SI: distance/elevation in m, speed in m/s.
        launch { value(DataType.Type.DISTANCE).collect { live.distanceM = it } }
        launch { value(DataType.Type.ELEVATION_GAIN).collect { live.elevGainM = it } }
        launch { value(DataType.Type.AVERAGE_SPEED).collect { live.avgSpeedMps = it } }
        launch { value(DataType.Type.MAX_SPEED).collect { live.maxSpeedMps = it } }
        launch { value(DataType.Type.ELAPSED_TIME).collect { live.elapsedSec = it } }
        launch { value(DataType.Type.AVERAGE_HR).collect { live.avgHr = it } }
        launch { value(DataType.Type.MAX_HR).collect { live.maxHr = it } }
        launch { value(DataType.Type.AVERAGE_POWER).collect { live.avgPower = it } }
        launch { value(DataType.Type.INTENSITY_FACTOR).collect { live.intensityFactor = it } }

        // Sample GPS through the ride (tagged with distance) so we can name it by its
        // midpoint, not its start — better for point-to-point rides. Throttled to ~100 m.
        launch {
            var lastSampledM = -1.0
            karoo.streamLocation().collect { loc ->
                val d = live.distanceM
                if (lastSampledM < 0 || d - lastSampledM >= 100.0) {
                    live.addSample(d, loc.lat, loc.lng)
                    lastSampledM = d
                }
            }
        }
    }

    /** On the first GPS fix, grab the weather (the place name is resolved later, at ride end). */
    private suspend fun captureWeather() {
        val loc = withTimeoutOrNull(60_000) { karoo.streamLocation().first() } ?: run {
            Log.i(TAG, "No GPS fix for weather"); return
        }
        WeatherClient(karoo).fetch(loc.lat, loc.lng)?.let {
            live.weather = it
            Log.i(TAG, "Weather captured: ${it.temperatureC}C code=${it.weatherCode}")
        }
    }

    private suspend fun processStrava() {
        val client = StravaClient(store, karooHttpEngine(karoo))
        val result = runCatching { client.processPending() }.getOrNull() ?: return
        result.applied.forEach { store.updateHistoryStatus(it.rideEndEpochMs, StravaStatus.APPLIED) }
    }

    private suspend fun retryStravaLoop() {
        while (true) {
            delay(2 * 60 * 1000L)
            if (store.pendingRenames.first().isNotEmpty()) processStrava()
        }
    }

    private fun showNotification(name: String, stats: RideStats) {
        karoo.dispatch(
            SystemNotification(
                id = UUID.randomUUID().toString(),
                message = name,
                subText = "%.0f km · %.0f m climbed".format(stats.distanceKm, stats.elevationGainM),
                header = "Ride named 🚴",
            ),
        )
    }

    /** Mutable accumulator for the in-progress ride. Updated by stream collectors. */
    private class LiveRide {
        @Volatile var startMs = 0L
        @Volatile var distanceM = 0.0
        @Volatile var elevGainM = 0.0
        @Volatile var avgSpeedMps = 0.0
        @Volatile var maxSpeedMps = 0.0
        @Volatile var elapsedSec = 0.0
        @Volatile var avgHr: Double? = null
        @Volatile var maxHr: Double? = null
        @Volatile var avgPower: Double? = null
        @Volatile var intensityFactor: Double? = null
        @Volatile var weather: WeatherSnapshot? = null
        @Volatile var placeName: String? = null

        private data class LocSample(val distanceM: Double, val lat: Double, val lng: Double)
        private val samples = java.util.Collections.synchronizedList(ArrayList<LocSample>())

        fun reset(nowMs: Long) {
            startMs = nowMs
            distanceM = 0.0; elevGainM = 0.0; avgSpeedMps = 0.0; maxSpeedMps = 0.0; elapsedSec = 0.0
            avgHr = null; maxHr = null; avgPower = null; intensityFactor = null
            weather = null; placeName = null
            samples.clear()
        }

        fun addSample(distanceM: Double, lat: Double, lng: Double) {
            if (samples.size < 10_000) samples.add(LocSample(distanceM, lat, lng))
        }

        /** Coordinate of the sample closest to the halfway-by-distance point. */
        fun midpoint(): Pair<Double, Double>? {
            val snapshot = synchronized(samples) { samples.toList() }
            if (snapshot.isEmpty()) return null
            val target = snapshot.last().distanceM / 2.0
            val s = snapshot.minByOrNull { kotlin.math.abs(it.distanceM - target) } ?: return null
            return s.lat to s.lng
        }

        fun toStats(endMs: Long) = RideStats(
            distanceKm = distanceM / 1000.0,
            elevationGainM = elevGainM,
            durationSec = if (elapsedSec > 0) elapsedSec else (endMs - startMs) / 1000.0,
            avgSpeedKmh = avgSpeedMps * 3.6,
            maxSpeedKmh = maxSpeedMps * 3.6,
            avgHr = avgHr,
            maxHr = maxHr,
            avgPower = avgPower,
            intensityFactor = intensityFactor,
            startEpochMs = startMs,
            endEpochMs = endMs,
            weather = weather,
            placeName = placeName,
        )
    }

    companion object {
        private const val TAG = "RideNamer"
    }
}
