package com.radiocoche.launcher.radar

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/** Volumen general (0–30) al que suenan los avisos de radar. Lo elige el usuario; empieza en 10. */
object AlertVolume {
    const val DEFAULT = 10
    const val MIN = 1
    const val MAX = 30

    private var loaded = false
    var level by mutableIntStateOf(DEFAULT)
        private set

    fun get(ctx: Context): Int {
        if (!loaded) {
            loaded = true
            level = ctx.getSharedPreferences("launcher", Context.MODE_PRIVATE).getInt("alert_volume", DEFAULT).coerceIn(MIN, MAX)
        }
        return level
    }

    fun change(ctx: Context, delta: Int) {
        val v = (get(ctx) + delta).coerceIn(MIN, MAX)
        level = v
        ctx.getSharedPreferences("launcher", Context.MODE_PRIVATE).edit().putInt("alert_volume", v).apply()
    }
}
