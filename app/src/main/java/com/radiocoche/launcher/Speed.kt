package com.radiocoche.launcher

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat

/** Velocidad del coche a partir del GPS de la unidad. */
class Speed(private val ctx: Context) {

    /** Velocidad en km/h (0 si no hay señal). */
    var kmh by mutableStateOf(0f)
        private set

    /** Hay señal de GPS reciente. */
    var hasFix by mutableStateOf(false)
        private set

    /** Última posición recibida (para guardar un radar). */
    var last: Location? = null
        private set

    private var lastFix = 0L
    private var running = false

    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            lastFix = SystemClock.elapsedRealtime()
            last = location
            hasFix = true
            val v = if (location.hasSpeed()) location.speed * 3.6f else 0f
            // Parado, el GPS baila entre 0 y 2 km/h: se deja en 0.
            kmh = if (v < 2.5f) 0f else v
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun start() {
        if (running || !hasPermission()) return
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        running = runCatching {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500L, 0f, listener, Looper.getMainLooper())
        }.isSuccess
    }

    fun stop() {
        if (!running) return
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        runCatching { lm.removeUpdates(listener) }
        running = false
    }

    /** Si llevan unos segundos sin llegar datos (túnel, garaje), se pone a 0. */
    fun tick() {
        if (hasFix && SystemClock.elapsedRealtime() - lastFix > 4000) {
            hasFix = false
            kmh = 0f
        }
    }
}
