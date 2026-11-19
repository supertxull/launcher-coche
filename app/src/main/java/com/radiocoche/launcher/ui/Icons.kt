package com.radiocoche.launcher.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Un trazo del icono: su forma, si va relleno, si lleva línea y su grosor. */
private class P(val d: String, val fill: Boolean = false, val stroke: Boolean = true, val w: Float = 2f)

private fun icon(name: String, vararg parts: P): ImageVector {
    val b = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
    for (p in parts) {
        b.addPath(
            pathData = PathParser().parsePathString(p.d).toNodes(),
            fill = if (p.fill) SolidColor(Color.White) else null,
            stroke = if (p.stroke) SolidColor(Color.White) else null,
            strokeLineWidth = p.w,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
    return b.build()
}

/** Iconos dibujados a mano, los mismos del diseño. */
object Ic {
    val Prev = icon("prev", P("M5 5v14", w = 2.4f), P("M19 5L9 12l10 7z", fill = true, w = 2.4f))
    val Next = icon("next", P("M5 5l10 7-10 7z", fill = true, w = 2.4f), P("M19 5v14", w = 2.4f))
    val Play = icon("play", P("M7 4l13 8-13 8z", fill = true, stroke = false))
    val Pause = icon("pause", P("M6 4h4v16h-4z", fill = true, stroke = false), P("M14 4h4v16h-4z", fill = true, stroke = false))
    val Note = icon(
        "note",
        P("M9 18V5l12-2v13", w = 1.8f),
        P("M3 18a3 3 0 1 0 6 0a3 3 0 1 0 -6 0", w = 1.8f),
        P("M15 16a3 3 0 1 0 6 0a3 3 0 1 0 -6 0", w = 1.8f),
    )
    val Radio = icon(
        "radio",
        P("M5 8h14a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-8a2 2 0 0 1 2-2z"),
        P("M7 8l10-5"),
        P("M6 14a2 2 0 1 0 4 0a2 2 0 1 0 -4 0"),
        P("M13 12h5M13 16h5"),
    )
    /** Coche: Android Auto (Tlink5). */
    val Car = icon(
        "car",
        P("M19 17h2c.6 0 1-.4 1-1v-3c0-.9-.7-1.7-1.5-1.9C18.7 10.6 16 10 16 10s-1.3-1.4-2.2-2.3c-.5-.4-1.1-.7-1.8-.7H5c-.6 0-1.1.4-1.4.9l-1.4 2.9A3.7 3.7 0 0 0 2 12v4c0 .6.4 1 1 1h2"),
        P("M5 17a2 2 0 1 0 4 0a2 2 0 1 0 -4 0"),
        P("M15 17a2 2 0 1 0 4 0a2 2 0 1 0 -4 0"),
        P("M9 17h6"),
    )
    val Nav = icon("nav", P("M3 11l19-9-9 19-2-8-8-2z"))
    val Phone = icon(
        "phone",
        P("M22 16.9v3a2 2 0 0 1-2.2 2 19.8 19.8 0 0 1-8.6-3.1 19.5 19.5 0 0 1-6-6A19.8 19.8 0 0 1 2.1 4.2 2 2 0 0 1 4.1 2h3a2 2 0 0 1 2 1.7c.1.9.4 1.8.7 2.7a2 2 0 0 1-.5 2.1L8 9.8a16 16 0 0 0 6 6l1.3-1.3a2 2 0 0 1 2.1-.4c.9.3 1.8.6 2.7.7a2 2 0 0 1 1.7 2z"),
    )
    val Apps = icon(
        "apps",
        P("M4 3h5a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z"),
        P("M15 3h5a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1h-5a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z"),
        P("M4 14h5a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1v-5a1 1 0 0 1 1-1z"),
        P("M15 14h5a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1h-5a1 1 0 0 1-1-1v-5a1 1 0 0 1 1-1z"),
    )
    val Gear = icon(
        "gear",
        P("M9 12a3 3 0 1 0 6 0a3 3 0 1 0 -6 0", w = 1.8f),
        P(
            "M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z",
            w = 1.8f,
        ),
    )
    val Back = icon("back", P("M15 5l-7 7 7 7", w = 2.4f))

    // Avisador de radares
    val Lights = icon("luces", P("M13 5c4 0 7 3.1 7 7s-3 7-7 7c-1.2 0-2-0.9-2-2V7c0-1.1 0.8-2 2-2z"), P("M3 8l5 1M3 12h5M3 16l5-1"))
    val Gps = icon("gps", P("M6 12a6 6 0 1 0 12 0a6 6 0 1 0 -12 0"), P("M10.4 12a1.6 1.6 0 1 0 3.2 0a1.6 1.6 0 1 0 -3.2 0"), P("M12 2v3M12 19v3M2 12h3M19 12h3"))
    val GpsOff = icon("gps_no", P("M6 12a6 6 0 1 0 12 0a6 6 0 1 0 -12 0"), P("M12 2v3M12 19v3M2 12h3M19 12h3"), P("M4 4l16 16"))
    val PinPlus = icon("pin_mas", P("M12 21s-7-6.2-7-11a7 7 0 0 1 14 0c0 4.8-7 11-7 11z", w = 1.9f), P("M12 7v6M9 10h6", w = 1.9f))
    val Refresh = icon("actualizar", P("M21 12a9 9 0 1 1-2.6-6.4", w = 1.9f), P("M21 3v6h-6", w = 1.9f))
    val ListIcon = icon("lista", P("M8 6h13M8 12h13M8 18h13", w = 1.9f), P("M3 6h.01M3 12h.01M3 18h.01", w = 2.6f))
    val Close = icon("cerrar", P("M6 6l12 12M18 6L6 18", w = 2.4f))
    val Pencil = icon("editar", P("M12 20h9"), P("M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z"))
    val Trash = icon("borrar", P("M3 6h18M8 6V4h8v2M19 6l-1 14H6L5 6M10 11v6M14 11v6"))
    val MutedSpeaker = icon("silencio", P("M11 5L6 9H2v6h4l5 4z"), P("M23 9l-6 6M17 9l6 6"))
}
