package com.radiocoche.launcher

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.radiocoche.launcher.ui.HomeScreen
import com.radiocoche.launcher.ui.ScaledRoot
import com.radiocoche.launcher.ui.Tema

class MainActivity : ComponentActivity() {

    private lateinit var home: HomeState

    private val askLocation = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        home.speed.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Tema.loadFonts(applicationContext)
        home = HomeState(applicationContext)
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
        home.onResume()
    }

    override fun onPause() {
        home.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        home.radio.stop()
        home.music.release()
        super.onDestroy()
    }
}
