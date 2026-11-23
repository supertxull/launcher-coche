package com.radiocoche.launcher.radar

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.content.ContextCompat
import com.radiocoche.launcher.R

/**
 * Avisador de radares, siempre en marcha (servicio en primer plano).
 * Lee el GPS por su cuenta, así funciona con cualquier navegador delante o sin ninguno.
 */
class RadarService : Service() {

    private lateinit var runner: AlertRunner
    private lateinit var engine: RadarEngine
    private val handler = Handler(Looper.getMainLooper())
    private var listening = false

    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            runCatching { engine.onLocation(location) }
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    /** Cada 10 minutos: ¿toca actualizar la lista? (con wifi, una vez al día). */
    private val updateCheck = object : Runnable {
        override fun run() {
            RadarStore.autoUpdate(this@RadarService)
            handler.postDelayed(this, 10 * 60 * 1000L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        // Conexión con el microcontrolador abierta desde ya (la usa el silencio de la radio).
        com.radiocoche.launcher.ui.CarLights.start(applicationContext)
        runner = AlertRunner(this)
        engine = RadarEngine(runner)
        RadarStore.load(this)
        goForeground()
        RadarState.running = true
        RadarState.test = {
            val a = Alert(Kind.FIXED, 50, Phase.APPROACH, distance = 500, speed = 0)
            RadarState.alert = a
            runner.approach(a)
            handler.postDelayed({ if (RadarState.alert == a) RadarState.alert = null }, 6000)
        }
        handler.postDelayed(updateCheck, 20 * 1000L)
        // El Teléfono pide silencio mientras hay una llamada (sonando, marcando o en curso).
        androidx.core.content.ContextCompat.registerReceiver(
            this, silenceReceiver, android.content.IntentFilter(ACTION_SILENCE),
            androidx.core.content.ContextCompat.RECEIVER_EXPORTED,
        )
    }

    private val silenceReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(c: android.content.Context, i: Intent) {
            runner.silence(i.getBooleanExtra("on", false))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startGps()
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        runCatching { unregisterReceiver(silenceReceiver) }
        if (listening) {
            val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            runCatching { lm.removeUpdates(listener) }
        }
        runner.release()
        RadarState.running = false
        RadarState.test = null
        RadarState.alert = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("MissingPermission")
    private fun startGps() {
        if (listening) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        listening = runCatching {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, listener, Looper.getMainLooper())
        }.isSuccess
    }

    private fun goForeground() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Avisador de radares", NotificationManager.IMPORTANCE_MIN))
        }
        val n: Notification = builder()
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("Avisador de radares")
            .setContentText("Activo")
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(ID, n)
        }
    }

    @Suppress("DEPRECATION")
    private fun builder(): Notification.Builder =
        if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, CHANNEL) else Notification.Builder(this)

    companion object {
        /** Aviso del Teléfono: extra "on" = true silencia las fuentes, false las devuelve. */
        const val ACTION_SILENCE = "com.radiocoche.launcher.SILENCE"

        private const val CHANNEL = "radares"
        private const val ID = 7

        fun start(ctx: Context) {
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
            runCatching { ContextCompat.startForegroundService(ctx, Intent(ctx, RadarService::class.java)) }
        }
    }
}

/** Arranca el avisador al encender la unidad. */
class RadarBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        RadarService.start(context)
    }
}
