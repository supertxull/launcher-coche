package com.radiocoche.launcher

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.radiocoche.launcher.ui.DayNight
import com.radiocoche.launcher.ui.HomeScreen
import com.radiocoche.launcher.ui.ScaledRoot

class MainActivity : ComponentActivity() {

    private lateinit var home: HomeState
    private val mainHandler = Handler(Looper.getMainLooper())

    /** Al encender o apagar las luces, la unidad avisa y cambiamos los colores. */
    private val dayNightReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            DayNight.apply()
            mainHandler.postDelayed({ DayNight.apply() }, 500)
        }
    }

    /** Mira el modo día/noche cada pocos segundos mientras se ve la pantalla. */
    private val dayNightPoll = object : Runnable {
        override fun run() {
            DayNight.apply()
            mainHandler.postDelayed(this, DayNight.POLL_MS)
        }
    }

    private val askLocation = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        home.speed.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DayNight.apply()
        home = HomeState(applicationContext)

        ContextCompat.registerReceiver(this, dayNightReceiver, IntentFilter(DayNight.ACTION), ContextCompat.RECEIVER_EXPORTED)
        home.radio.start()

        setContent {
            // En la pantalla de inicio, atrás solo cierra lo que haya abierto encima.
            BackHandler(enabled = true) { home.closeOverlays() }
            ScaledRoot { HomeScreen(home) }
        }

        if (!home.speed.hasPermission()) {
            askLocation.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    /** Tecla HOME estando ya en la pantalla de inicio. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        home.closeOverlays()
    }

    override fun onResume() {
        super.onResume()
        mainHandler.removeCallbacks(dayNightPoll)
        dayNightPoll.run()
        home.onResume()
    }

    override fun onPause() {
        mainHandler.removeCallbacks(dayNightPoll)
        home.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        runCatching { unregisterReceiver(dayNightReceiver) }
        home.radio.stop()
        home.music.release()
        super.onDestroy()
    }
}
