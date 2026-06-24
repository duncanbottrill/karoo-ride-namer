package com.duncanbottrill.ridenamer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duncanbottrill.ridenamer.data.HistoryEntry
import com.duncanbottrill.ridenamer.data.RideNamerStore
import com.duncanbottrill.ridenamer.data.StravaCredentials
import com.duncanbottrill.ridenamer.data.StravaStatus
import com.duncanbottrill.ridenamer.name.NameStyle
import com.duncanbottrill.ridenamer.name.SAMPLE_RIDE_STATS
import com.duncanbottrill.ridenamer.name.generateRideName
import com.duncanbottrill.ridenamer.strava.StravaClient
import com.duncanbottrill.ridenamer.strava.directHttpEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Accent = Color(0xFFFF5A1F)

@Composable
fun RideNamerApp(
    store: RideNamerStore,
    statusMessage: MutableState<String?>,
    onConnectStrava: () -> Unit,
) {
    MaterialTheme(colorScheme = darkColorScheme(primary = Accent)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold { padding ->
                val history by store.history.collectAsState(initial = emptyList())
                val creds by store.stravaCredentials.collectAsState(initial = StravaCredentials())
                val style by store.nameStyle.collectAsState(initial = NameStyle.FUNNY)
                val scope = rememberCoroutineScope()

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item { Header() }
                    item {
                        StyleCard(current = style, onSelect = { picked ->
                            scope.launch(Dispatchers.IO) { store.setNameStyle(picked) }
                        })
                    }
                    item { DemoCard(style) }
                    item { StravaCard(store, creds, statusMessage, onConnectStrava) }
                    item {
                        Text(
                            "History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    if (history.isEmpty()) {
                        item {
                            Text(
                                "No rides named yet. Finish a ride on your Karoo and the name will show up here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        items(history) { HistoryRow(it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header() {
    Column {
        Text("Ride Namer", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Accent)
        Text(
            "Finish a ride; get a name you didn't ask for.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StyleCard(current: NameStyle, onSelect: (NameStyle) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Name style", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NameStyle.entries.forEach { option ->
                    val selected = option == current
                    val modifier = Modifier.weight(1f)
                    if (selected) {
                        Button(onClick = { onSelect(option) }, modifier = modifier) { Text(option.label) }
                    } else {
                        OutlinedButton(onClick = { onSelect(option) }, modifier = modifier) { Text(option.label) }
                    }
                }
            }
            Text(
                current.blurb,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DemoCard(style: NameStyle) {
    var seed by remember { mutableStateOf(System.nanoTime()) }
    val sample = remember(style, seed) { generateRideName(SAMPLE_RIDE_STATS, style, seed) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Sample", style = MaterialTheme.typography.labelMedium, color = Accent)
            Text(
                sample,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { seed = System.nanoTime() },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Shuffle 🎲") }
        }
    }
}

@Composable
private fun StravaCard(
    store: RideNamerStore,
    creds: StravaCredentials,
    statusMessage: MutableState<String?>,
    onConnectStrava: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Strava auto-rename", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            statusMessage.value?.let {
                Text(it, color = Accent, style = MaterialTheme.typography.bodyMedium)
            }

            if (creds.isConnected) {
                Text("Connected ✓ — finished rides will be renamed once they sync to Strava.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        scope.launch(Dispatchers.IO) {
                            val res = StravaClient(store, directHttpEngine()).processPending()
                            res.applied.forEach { store.updateHistoryStatus(it.rideEndEpochMs, StravaStatus.APPLIED) }
                            statusMessage.value = "Synced: ${res.applied.size} renamed, ${res.stillPending.size} waiting."
                        }
                    }) { Text("Sync now") }
                    OutlinedButton(onClick = {
                        scope.launch(Dispatchers.IO) {
                            store.setStravaCredentials(creds.copy(refreshToken = "", accessToken = ""))
                            statusMessage.value = "Disconnected."
                        }
                    }) { Text("Disconnect") }
                }
            } else {
                Text(
                    "Connect your Strava account so finished rides get renamed automatically once " +
                        "they sync. Tapping below opens Strava to approve access.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = onConnectStrava,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Connect with Strava") }
            }
        }
    }
}

private val dateFormat = SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault())

@Composable
private fun HistoryRow(entry: HistoryEntry) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(entry.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "%.0f km · %.0f m · %s".format(
                    entry.stats.distanceKm,
                    entry.stats.elevationGainM,
                    dateFormat.format(Date(entry.generatedAtMs)),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(stravaLabel(entry.stravaStatus), style = MaterialTheme.typography.labelSmall, color = Accent)
        }
    }
}

private fun stravaLabel(status: StravaStatus) = when (status) {
    StravaStatus.NOT_CONFIGURED -> ""
    StravaStatus.PENDING -> "Strava: waiting to rename…"
    StravaStatus.APPLIED -> "Strava: renamed ✓"
    StravaStatus.FAILED -> "Strava: rename failed"
}
