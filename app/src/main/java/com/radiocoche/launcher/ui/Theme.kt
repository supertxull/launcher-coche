package com.radiocoche.launcher.ui

import android.content.Context
import android.graphics.Typeface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/** Colores de la app: el tema "contraste alto" de la Radio y Música, con su naranja y su rojo. */
data class Palette(
    val bg: Color,
    val panel: Color,
    val raised: Color,
    val border: Color,
    val border2: Color,
    val text: Color,
    val muted: Color,
    val dim: Color,
    val accent: Color,
    val accentBg: Color,
    val track: Color,
)

/** Luces apagadas: contraste alto, para el sol. */
val LauncherPalette = Palette(
    bg = Color(0xFF000000), panel = Color(0xFF1C2230), raised = Color(0xFF2A3242),
    border = Color(0xFF56627A), border2 = Color(0xFF6E7B94),
    text = Color(0xFFFFFFFF), muted = Color(0xFFDCE2EA), dim = Color(0xFFA9B3C2),
    accent = Color(0xFFFF6A1A), accentBg = Color(0xFF4A2008),
    track = Color(0xFF6E7B94),
)

/** Luces encendidas: los colores oscuros de la Radio y Música. */
val NightPalette = Palette(
    bg = Color(0xFF0B0E13), panel = Color(0xFF151A22), raised = Color(0xFF1B212B),
    border = Color(0xFF232A35), border2 = Color(0xFF2A3240),
    text = Color(0xFFF2F4F7), muted = Color(0xFFA3ADBB), dim = Color(0xFF7A8494),
    accent = Color(0xFFFF6A1A), accentBg = Color(0xFF2B140A),
    track = Color(0xFF343C4A),
)

/** Colores según las luces del coche ([CarLights]); si aún no se sabe, contraste alto. */
object DayNight {
    fun isNight(): Boolean = CarLights.on == true

    fun apply() {
        val next = if (isNight()) NightPalette else LauncherPalette
        if (Tema.p !== next) Tema.p = next
    }
}

/** Rojo de la aguja del velocímetro y de la marca del dial. */
val NeedleRed = Color(0xFFFF0000)

object Tema {
    var p by mutableStateOf(LauncherPalette)

    /** Letra del texto (Barlow). */
    var body: FontFamily = FontFamily.SansSerif
        private set

    /** Letra estrecha de los números grandes (Barlow Condensed). */
    var condensed: FontFamily = FontFamily(Typeface.create("sans-serif-condensed", Typeface.BOLD))
        private set

    /** Typeface para dibujar los números del velocímetro. */
    var condensedTypeface: Typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        private set

    /** Carga la letra Barlow que va dentro de la app (si no está, se queda la del sistema). */
    fun loadFonts(ctx: Context) {
        val have = runCatching { ctx.assets.list("fonts")?.toSet() }.getOrNull() ?: emptySet()
        fun f(name: String, weight: Int): Font? =
            if ("$name.ttf" in have) Font("fonts/$name.ttf", ctx.assets, FontWeight(weight)) else null
        val bodyFonts = listOfNotNull(
            f("Barlow-Regular", 400), f("Barlow-Medium", 500), f("Barlow-SemiBold", 600), f("Barlow-Bold", 700),
        )
        if (bodyFonts.isNotEmpty()) body = FontFamily(bodyFonts)
        val condFonts = listOfNotNull(f("BarlowCondensed-SemiBold", 600), f("BarlowCondensed-Bold", 700))
        if (condFonts.isNotEmpty()) condensed = FontFamily(condFonts)
        if ("BarlowCondensed-Bold.ttf" in have) {
            runCatching { Typeface.createFromAsset(ctx.assets, "fonts/BarlowCondensed-Bold.ttf") }
                .getOrNull()?.let { condensedTypeface = it }
        }
    }
}
