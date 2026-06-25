package com.duncanbottrill.ridenamer.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.duncanbottrill.ridenamer.data.RideNamerStore
import com.duncanbottrill.ridenamer.strava.StravaRenameWorker

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = RideNamerStore(applicationContext)
        // Nudge any pending Strava rename when the app is opened.
        StravaRenameWorker.schedule(applicationContext)
        setContent {
            RideNamerApp(store)
        }
    }
}
