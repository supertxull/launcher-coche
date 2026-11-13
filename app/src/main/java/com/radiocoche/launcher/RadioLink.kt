package com.radiocoche.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat

/**
 * Lo que suena en la app Radio y sus mandos.
 *
 * La Radio envía su estado con el aviso [STATE] cada vez que cambia y obedece las
 * órdenes del aviso [CMD] ("query", "prev", "next", "preset" + "index").
 */
class RadioLink(private val ctx: Context) {

    /** La Radio está abierta (en marcha) y ha respondido. */
    var alive by mutableStateOf(false)
        private set
    var bank by mutableStateOf("")
        private set
    var freq by mutableStateOf("")
        private set
    var unit by mutableStateOf("MHz")
        private set
    var name by mutableStateOf("")
        private set
    var text by mutableStateOf("")
        private set
    var pos by mutableStateOf(0.5f)
        private set
    var labels by mutableStateOf(listOf("87.5", "91.6", "95.7", "99.8", "103.9", "108.0"))
        private set
    var preset by mutableStateOf(-1)
        private set
    var presetFreqs by mutableStateOf(emptyList<String>())
        private set
    var presetNames by mutableStateOf(emptyList<String>())
        private set

    /** Se llama cuando la Radio pasa a primer plano. */
    var onRadioFront: (() -> Unit)? = null

    private val prefs = ctx.getSharedPreferences("radio", Context.MODE_PRIVATE)
    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, i: Intent) {
            alive = true
            bank = i.getStringExtra("bank") ?: ""
            freq = i.getStringExtra("freq") ?: ""
            unit = i.getStringExtra("unit") ?: "MHz"
            name = i.getStringExtra("name") ?: ""
            text = i.getStringExtra("text") ?: ""
            pos = i.getFloatExtra("pos", 0.5f)
            i.getStringArrayExtra("labels")?.let { labels = it.toList() }
            preset = i.getIntExtra("preset", -1)
            presetFreqs = i.getStringArrayExtra("pFreq")?.toList() ?: emptyList()
            presetNames = i.getStringArrayExtra("pName")?.toList() ?: emptyList()
            prefs.edit()
                .putString("bank", bank).putString("freq", freq).putString("unit", unit)
                .putString("name", name).putFloat("pos", pos)
                .apply()
            if (i.getBooleanExtra("front", false)) onRadioFront?.invoke()
        }
    }

    init {
        // Lo último que se supo, por si la Radio no está abierta.
        bank = prefs.getString("bank", "") ?: ""
        freq = prefs.getString("freq", "") ?: ""
        unit = prefs.getString("unit", "MHz") ?: "MHz"
        name = prefs.getString("name", "") ?: ""
        pos = prefs.getFloat("pos", 0.5f)
    }

    fun start() {
        if (registered) return
        ContextCompat.registerReceiver(ctx, receiver, IntentFilter(STATE), ContextCompat.RECEIVER_EXPORTED)
        registered = true
    }

    fun stop() {
        if (!registered) return
        runCatching { ctx.unregisterReceiver(receiver) }
        registered = false
    }

    /** Pregunta a la Radio su estado. Si no contesta, es que no está abierta. */
    fun query() {
        alive = false
        send("query")
    }

    fun previous() = send("prev")
    fun next() = send("next")
    fun selectPreset(index: Int) = send("preset", index)

    private fun send(cmd: String, index: Int = -1) {
        ctx.sendBroadcast(Intent(CMD).setPackage(Pkg.RADIO).putExtra("cmd", cmd).putExtra("index", index))
    }

    companion object {
        const val STATE = "com.radiocoche.radio.STATE"
        const val CMD = "com.radiocoche.radio.CMD"
    }
}
