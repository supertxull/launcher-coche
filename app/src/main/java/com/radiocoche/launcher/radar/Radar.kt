package com.radiocoche.launcher.radar

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Tipos de radar. [label] es lo que se lee en el cartel. */
enum class Kind(val label: String, val code: String) {
    FIXED("Radar fijo", "fijo"),
    SECTION("Radar de tramo", "tramo"),
    RED_LIGHT("Semáforo", "semaforo"),
    MOBILE("Radar móvil", "movil"),
    MOBILE_RECURRING("Radar móvil frecuente", "movil_frecuente"),
    MOBILE_STRETCH("Tramo vigilado", "tramo_vigilado"),
    EXTRA("Extra", "extra");

    companion object {
        fun fromCode(c: String?): Kind = entries.firstOrNull { it.code.equals(c?.trim(), ignoreCase = true) } ?: EXTRA

        /** Los tipos que puede tener un radar añadido por el usuario. */
        val USER = listOf(FIXED, RED_LIGHT, MOBILE, EXTRA)
    }
}

/**
 * Un radar.
 * [bearing]: sentido de marcha que vigila (grados, 0 = norte) o null si vigila los dos.
 * [sectionId] y [sectionEnd]: en los radares de tramo, a qué tramo pertenece y qué extremo es (0 o 1).
 */
data class Radar(
    val id: String,
    val lat: Double,
    val lon: Double,
    val kind: Kind,
    val limit: Int?,
    val bearing: Float?,
    val sectionId: String? = null,
    val sectionEnd: Int = 0,
    val user: Boolean = false,
    val date: String = "",
)

object Geo {
    private const val R = 6371000.0

    fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * R * atan2(sqrt(a), sqrt(1 - a))
    }

    /** Rumbo de 1 a 2 en grados (0 = norte, 90 = este). */
    fun bearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val p1 = Math.toRadians(lat1)
        val p2 = Math.toRadians(lat2)
        val dl = Math.toRadians(lon2 - lon1)
        val y = sin(dl) * cos(p2)
        val x = cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dl)
        return ((Math.toDegrees(atan2(y, x)) + 360) % 360).toFloat()
    }

    /** Diferencia entre dos rumbos, de 0 a 180. */
    fun angle(a: Float, b: Float): Float {
        val d = abs(a - b) % 360
        return if (d > 180) 360 - d else d
    }

    /** "N", "NE"… para un rumbo. */
    fun compass(b: Float?): String {
        if (b == null) return "ambos"
        val names = arrayOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")
        return names[(((b + 22.5f) % 360) / 45f).toInt().coerceIn(0, 7)]
    }
}
