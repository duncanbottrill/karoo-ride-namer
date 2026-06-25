package com.duncanbottrill.ridenamer.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.duncanbottrill.ridenamer.data.RideNamerStore

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = RideNamerStore(applicationContext)
        setContent {
            RideNamerApp(store)
        }
    }
}
