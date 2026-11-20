package com.radiocoche.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import com.radiocoche.launcher.radar.Kind
import com.radiocoche.launcher.radar.RadarService
import com.radiocoche.launcher.radar.RadarStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Source { MUSICA, RADIO, BT }

/** Pantallas de la ventana de radares. */
enum class RadarView { MAIN, LIMIT, LIST, EDIT }

/** Posición guardada al tocar el indicador del GPS. */
class Snap(val lat: Double, val lon: Double, val bearing: Float?, val time: Long)

/** Todo lo que muestra la pantalla de inicio. */
class HomeState(private val ctx: Context) {

    private val prefs = ctx.getSharedPreferences("launcher", Context.MODE_PRIVATE)

    val speed = Speed(ctx)
    val music = MusicLink(ctx)
    val radio = RadioLink(ctx)
    val bt = BtLink(ctx)

    /** Qué se ve en el panel derecho: lo último que se usó. */
    var source by mutableStateOf(
        Source.entries.firstOrNull { it.name == prefs.getString("source", "") } ?: Source.MUSICA
    )
        private set

    var time by mutableStateOf("")
        private set
    var date by mutableStateOf("")
        private set

    var isDefaultHome by mutableStateOf(true)
        private set
    var showApps by mutableStateOf(false)
    var showNavChooser by mutableStateOf(false)

    /** Se puede pintar el aviso sobre otras apps. */
    var canOverlay by mutableStateOf(true)
        private set

    // Ventana de radares
    var showRadars by mutableStateOf(false)
    var radarView by mutableStateOf(RadarView.MAIN)
    var snap by mutableStateOf<Snap?>(null)
        private set
    var newRadarId by mutableStateOf<String?>(null)
        private set
    var editId by mutableStateOf<String?>(null)
        private set
    var editKind by mutableStateOf(Kind.EXTRA)
    var editLimit by mutableStateOf<Int?>(null)
    var toast by mutableStateOf("")
        private set
    private var toastUntil = 0L

    private val es = Locale("es", "ES")
    private val timeFormat = SimpleDateFormat("HH:mm", es)
    private val dateFormat = SimpleDateFormat("EEEE · dd-MM-yyyy", es)

    init {
        music.onStartedPlaying = { select(Source.MUSICA) }
        radio.onRadioFront = { select(Source.RADIO) }
        bt.onStartedPlaying = { select(Source.BT) }
        tick()
    }

    fun select(s: Source) {
        if (source == s) return
        source = s
        prefs.edit().putString("source", s.name).apply()
    }

    fun onResume() {
        isDefaultHome = Apps.isDefaultHome(ctx)
        canOverlay = Settings.canDrawOverlays(ctx)
        RadarService.start(ctx)
        speed.start()
        music.connect()
        radio.query()
        bt.start()
        bt.query()
        tick()
    }

    fun onPause() {
        speed.stop()
    }

    /** Dos veces por segundo: reloj, velocidad y barra de progreso. */
    fun tick() {
        val now = Date()
        time = timeFormat.format(now)
        date = dateFormat.format(now)
        speed.tick()
        music.tick()
        bt.tick()
        if (toast.isNotEmpty() && SystemClock.elapsedRealtime() > toastUntil) toast = ""
        if (RadarStore.message.isNotEmpty()) {
            showToast(RadarStore.message)
            RadarStore.message = ""
        }
    }

    /** Tecla atrás o HOME: cierra lo que haya abierto encima. Devuelve true si cerró algo. */
    fun closeOverlays(): Boolean {
        val open = showApps || showNavChooser || showRadars
        showApps = false
        showNavChooser = false
        closeRadars()
        return open
    }

    // ------------------------------------------------------------ botones de la barra

    fun openRadio() {
        select(Source.RADIO)
        Apps.open(ctx, Pkg.RADIO, Pkg.RADIO_FABRICA)
    }

    fun openMusic() {
        select(Source.MUSICA)
        Apps.open(ctx, Pkg.MUSICA, Pkg.MUSICA_FABRICA)
    }

    fun openAndroidAuto() {
        Apps.open(ctx, Pkg.ZLINK)
    }

    fun openPhone() {
        Apps.open(ctx, Pkg.TELEFONO)
    }

    fun openSettings() {
        if (!Apps.open(ctx, Pkg.AJUSTES_UNIDAD)) Apps.openAndroidSettings(ctx)
    }

    fun openAndroidSettings() = Apps.openAndroidSettings(ctx)

    fun openHomeChooser() = Apps.openHomeChooser(ctx)

    /** Abre el navegador elegido; la primera vez pregunta cuál. */
    fun openNavigator() {
        val saved = prefs.getString("nav", null)?.let { ComponentName.unflattenFromString(it) }
        if (saved != null && ctx.packageManager.getLaunchIntentForPackage(saved.packageName) != null) {
            Apps.open(ctx, saved)
        } else {
            showNavChooser = true
        }
    }

    fun chooseNavigator(app: AppEntry) {
        prefs.edit().putString("nav", app.component.flattenToString()).apply()
        showNavChooser = false
        Apps.open(ctx, app.component)
    }

    fun openApp(app: AppEntry) {
        showApps = false
        Apps.open(ctx, app.component)
    }

    // ------------------------------------------------------------ mandos del panel

    /** Abre Música directamente en la pantalla de Bluetooth. */
    fun openBluetooth() {
        select(Source.BT)
        val i = ctx.packageManager.getLaunchIntentForPackage(Pkg.MUSICA)
        if (i != null) {
            runCatching { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("source", "BT")) }
        } else {
            Apps.open(ctx, Pkg.TELEFONO)
        }
    }

    fun radioPrevious() = radio.previous()
    fun radioNext() = radio.next()

    fun musicPlayPause() { if (!music.playPause()) openMusic() }
    fun musicNext() { if (!music.next()) openMusic() }
    fun musicPrevious() { if (!music.previous()) openMusic() }

    // ------------------------------------------------------------ avisador de radares

    /** Abre el permiso «Mostrar sobre otras apps». */
    fun openOverlaySettings() {
        runCatching {
            ctx.startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + ctx.packageName))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    private fun showToast(text: String) {
        toast = text
        toastUntil = SystemClock.elapsedRealtime() + 2600
    }

    /** Toque en el indicador del GPS: se guarda la posición de este instante y se abre la ventana. */
    fun openRadars() {
        val l = speed.last
        snap = if (speed.hasFix && l != null) {
            val moving = l.hasSpeed() && l.speed * 3.6f > 10f
            Snap(l.latitude, l.longitude, if (moving && l.hasBearing()) l.bearing else null, SystemClock.elapsedRealtime())
        } else null
        radarView = RadarView.MAIN
        showRadars = true
    }

    fun closeRadars() {
        showRadars = false
        snap = null
    }

    fun addRadarHere() {
        val s = snap ?: return
        newRadarId = RadarStore.addUser(ctx, s.lat, s.lon, s.bearing)
        radarView = RadarView.LIMIT
    }

    fun setNewRadarLimit(limit: Int?) {
        val id = newRadarId ?: return
        RadarStore.editUser(ctx, id, Kind.EXTRA, limit)
        radarView = RadarView.MAIN
        showToast(if (limit != null) "Radar guardado a $limit km/h" else "Radar guardado sin velocidad")
    }

    fun updateRadars() = RadarStore.update(ctx, manual = true)

    /** Aviso de prueba: pitidos, silencio y destello, sin moverse. */
    fun testAlert() {
        val t = com.radiocoche.launcher.radar.RadarState.test
        if (t == null) {
            showToast("El avisador no está en marcha")
            return
        }
        closeRadars()
        t()
    }

    fun startEdit(id: String) {
        val r = RadarStore.user.firstOrNull { it.id == id } ?: return
        editId = id
        editKind = r.kind
        editLimit = r.limit
        radarView = RadarView.EDIT
    }

    fun saveEdit() {
        editId?.let { RadarStore.editUser(ctx, it, editKind, editLimit) }
        radarView = RadarView.LIST
    }

    fun deleteRadar(id: String) = RadarStore.deleteUser(ctx, id)
}
