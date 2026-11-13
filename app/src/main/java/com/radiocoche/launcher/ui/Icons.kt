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
}
