package com.duncanbottrill.ridenamer.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import com.duncanbottrill.ridenamer.data.RideNamerStore
import com.duncanbottrill.ridenamer.strava.StravaClient
import com.duncanbottrill.ridenamer.strava.directHttpEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var store: RideNamerStore
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Surfaced to Compose so the OAuth result can be shown.
    private val statusMessage = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = RideNamerStore(applicationContext)
        importCredentialExtras(intent)
        handleDeepLink(intent)
        setContent {
            RideNamerApp(
                store = store,
                statusMessage = statusMessage,
                onConnectStrava = ::launchStravaAuth,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        importCredentialExtras(intent)
        handleDeepLink(intent)
    }

    /**
     * Imports Strava credentials passed as intent extras, so they can be set from a computer
     * over ADB instead of typed on the Karoo, e.g.:
     *
     *   adb shell am start -n com.duncanbottrill.ridenamer/.ui.MainActivity \
     *     -e strava_client_id 12345 \
     *     -e strava_client_secret <secret> \
     *     -e strava_connect true        # optional: also start authorization
     */
    private fun importCredentialExtras(intent: Intent?) {
        val id = intent?.getStringExtra("strava_client_id")?.trim()
        val secret = intent?.getStringExtra("strava_client_secret")?.trim()
        if (id.isNullOrBlank() && secret.isNullOrBlank()) return
        val autoConnect = intent?.getBooleanExtra("strava_connect", false) == true
        ioScope.launch {
            val creds = store.stravaCredentials.first()
            val newId = id?.takeIf { it.isNotBlank() } ?: creds.clientId
            val newSecret = secret?.takeIf { it.isNotBlank() } ?: creds.clientSecret
            store.setStravaCredentials(creds.copy(clientId = newId, clientSecret = newSecret))
            statusMessage.value = "Strava credentials imported."
            if (autoConnect && newId.isNotBlank() && newSecret.isNotBlank()) {
                launchStravaAuth(newId, newSecret)
            }
        }
    }

    /** Saves credentials then opens the Strava authorize page in the browser. */
    private fun launchStravaAuth(clientId: String, clientSecret: String) {
        ioScope.launch {
            val creds = store.stravaCredentials.first()
            store.setStravaCredentials(creds.copy(clientId = clientId.trim(), clientSecret = clientSecret.trim()))
            val url = StravaClient(store, directHttpEngine()).authorizeUrl(clientId.trim())
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    /** Captures the ?code=... from the ridenamer://strava-callback redirect. */
    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme != "ridenamer") return
        val error = data.getQueryParameter("error")
        if (error != null) {
            statusMessage.value = "Strava authorization failed: $error"
            return
        }
        val code = data.getQueryParameter("code") ?: return
        statusMessage.value = "Connecting to Strava…"
        ioScope.launch {
            val ok = StravaClient(store, directHttpEngine()).exchangeCode(code)
            statusMessage.value = if (ok) "Strava connected ✓" else "Couldn't connect to Strava — check your Client ID/Secret."
        }
    }
}
