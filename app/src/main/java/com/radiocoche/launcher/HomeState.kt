package com.radiocoche.launcher

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Source { MUSICA, RADIO }

/** Todo lo que muestra la pantalla de inicio. */
class HomeState(private val ctx: Context) {

    private val prefs = ctx.getSharedPreferences("launcher", Context.MODE_PRIVATE)

    val speed = Speed(ctx)
    val music = MusicLink(ctx)
    val radio = RadioLink(ctx)

    /** Qué se ve en el panel derecho: lo último que se usó. */
    var source by mutableStateOf(
        if (prefs.getString("source", "") == Source.RADIO.name) Source.RADIO else Source.MUSICA
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

    private val es = Locale("es", "ES")
    private val timeFormat = SimpleDateFormat("HH:mm", es)
    private val dateFormat = SimpleDateFormat("EEEE · dd-MM-yyyy", es)

    init {
        music.onStartedPlaying = { select(Source.MUSICA) }
        radio.onRadioFront = { select(Source.RADIO) }
        tick()
    }

    fun select(s: Source) {
        if (source == s) return
        source = s
        prefs.edit().putString("source", s.name).apply()
    }

    fun onResume() {
        isDefaultHome = Apps.isDefaultHome(ctx)
        speed.start()
        music.connect()
        radio.query()
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
    }

    /** Tecla atrás o HOME: cierra lo que haya abierto encima. Devuelve true si cerró algo. */
    fun closeOverlays(): Boolean {
        val open = showApps || showNavChooser
        showApps = false
        showNavChooser = false
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

    fun musicPlayPause() { if (!music.playPause()) openMusic() }
    fun musicNext() { if (!music.next()) openMusic() }
    fun musicPrevious() { if (!music.previous()) openMusic() }

    // La Radio (0.14 o posterior) atiende las órdenes aunque esté cerrada, así que se le mandan siempre.
    private fun radioInstalled() = ctx.packageManager.getLaunchIntentForPackage(Pkg.RADIO) != null

    fun radioNext() { if (radioInstalled()) radio.next() else openRadio() }
    fun radioPrevious() { if (radioInstalled()) radio.previous() else openRadio() }
    fun radioPreset(i: Int) { if (radioInstalled()) radio.selectPreset(i) else openRadio() }
}
