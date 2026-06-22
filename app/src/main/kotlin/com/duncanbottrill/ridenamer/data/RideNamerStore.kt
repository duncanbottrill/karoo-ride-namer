package com.duncanbottrill.ridenamer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ridenamer")

/**
 * Single source of truth for everything we persist: generated-name history, the
 * pending-Strava-rename queue, and Strava credentials. Lists are JSON-encoded into
 * preference strings to keep things dependency-light.
 */
class RideNamerStore(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private val keyHistory = stringPreferencesKey("history")
    private val keyPending = stringPreferencesKey("pending_renames")
    private val keyStrava = stringPreferencesKey("strava_credentials")

    // --- History ---

    val history: Flow<List<HistoryEntry>> = context.dataStore.data.map { prefs ->
        prefs[keyHistory]?.let { runCatching { json.decodeFromString<List<HistoryEntry>>(it) }.getOrNull() }
            ?: emptyList()
    }

    suspend fun addHistory(entry: HistoryEntry) {
        context.dataStore.edit { prefs ->
            val current = prefs[keyHistory]?.let {
                runCatching { json.decodeFromString<List<HistoryEntry>>(it) }.getOrNull()
            } ?: emptyList()
            val updated = (listOf(entry) + current).take(100)
            prefs[keyHistory] = json.encodeToString(updated)
        }
    }

    suspend fun updateHistoryStatus(generatedAtMs: Long, status: StravaStatus, newName: String? = null) {
        context.dataStore.edit { prefs ->
            val current = prefs[keyHistory]?.let {
                runCatching { json.decodeFromString<List<HistoryEntry>>(it) }.getOrNull()
            } ?: return@edit
            val updated = current.map {
                if (it.generatedAtMs == generatedAtMs) it.copy(stravaStatus = status, name = newName ?: it.name) else it
            }
            prefs[keyHistory] = json.encodeToString(updated)
        }
    }

    // --- Pending Strava renames ---

    val pendingRenames: Flow<List<PendingRename>> = context.dataStore.data.map { prefs ->
        prefs[keyPending]?.let { runCatching { json.decodeFromString<List<PendingRename>>(it) }.getOrNull() }
            ?: emptyList()
    }

    suspend fun setPendingRenames(list: List<PendingRename>) {
        context.dataStore.edit { it[keyPending] = json.encodeToString(list) }
    }

    suspend fun addPendingRename(rename: PendingRename) {
        context.dataStore.edit { prefs ->
            val current = prefs[keyPending]?.let {
                runCatching { json.decodeFromString<List<PendingRename>>(it) }.getOrNull()
            } ?: emptyList()
            prefs[keyPending] = json.encodeToString(current + rename)
        }
    }

    // --- Strava credentials ---

    val stravaCredentials: Flow<StravaCredentials> = context.dataStore.data.map { prefs ->
        prefs[keyStrava]?.let { runCatching { json.decodeFromString<StravaCredentials>(it) }.getOrNull() }
            ?: StravaCredentials()
    }

    suspend fun setStravaCredentials(creds: StravaCredentials) {
        context.dataStore.edit { it[keyStrava] = json.encodeToString(creds) }
    }
}
