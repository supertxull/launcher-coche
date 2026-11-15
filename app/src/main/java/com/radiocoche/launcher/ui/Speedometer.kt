package com.radiocoche.launcher.ui

import android.graphics.Paint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val MAX_SPEED = 240f
private val Ink = Color(0xFF160700)
private val TickMinor = Color(0xFF1A0802)
private val TickMajor = Color(0xFF120501)
private val BandEdge = Color(0xFFB83A08)

/**
 * Velocímetro de la propuesta 1 (franja naranja): franja mate de 0 a 240 km/h,
 * aguja roja gruesa con reborde negro y la velocidad en naranja sobre el círculo negro.
 */
@Composable
fun Speedometer(speedKmh: Float, modifier: Modifier) {
    val shown = speedKmh.coerceIn(0f, MAX_SPEED)
    val animated by animateFloatAsState(shown, tween(600), label = "aguja")
    val p = Tema.p
    val numbers = remember(Tema.condensedTypeface) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Tema.condensedTypeface
            textAlign = Paint.Align.CENTER
            color = Ink.toArgb()
        }
    }

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            // El dibujo está pensado en una rejilla de 400×400 (como el diseño).
            val u = size.minDimension / 400f
            val c = Offset(size.width / 2f, size.height / 2f)

            drawCircle(Color(0xFF0E0603), 198f * u, c)
            drawCircle(Color(0xFF2A1408), 198f * u, c, style = Stroke(3f * u))

            // Franja naranja mate, abierta por abajo (de 125° a 55°, pasando por arriba).
            drawArc(
                Brush.horizontalGradient(
                    listOf(BandEdge, p.accent, BandEdge),
                    startX = c.x - 192f * u, endX = c.x + 192f * u,
                ),
                125f, 290f, false,
                Offset(c.x - 160f * u, c.y - 160f * u), Size(320f * u, 320f * u),
                style = Stroke(64f * u),
            )

            // Rayitas: una cada 2 km/h y una gruesa cada 20.
            for (i in 0..120) {
                val a = Math.toRadians(135.0 + i * 2.25)
                val dx = cos(a).toFloat()
                val dy = sin(a).toFloat()
                val major = i % 10 == 0
                val r1 = (if (major) 176f else 182f) * u
                val r2 = (if (major) 192f else 190f) * u
                drawLine(
                    if (major) TickMajor else TickMinor,
                    Offset(c.x + dx * r1, c.y + dy * r1),
                    Offset(c.x + dx * r2, c.y + dy * r2),
                    strokeWidth = (if (major) 5f else 2f) * u,
                )
            }

            // Números de 20 en 20, pegados a las rayitas.
            numbers.textSize = 25f * u
            val baseline = -(numbers.ascent() + numbers.descent()) / 2f
            for (i in 0..12) {
                val a = Math.toRadians(135.0 + i * 22.5)
                val x = c.x + cos(a).toFloat() * 155f * u
                val y = c.y + sin(a).toFloat() * 155f * u
                drawContext.canvas.nativeCanvas.drawText((i * 20).toString(), x, y + baseline, numbers)
            }

            // Aguja: rectángulo rojo con reborde negro, desde el círculo hasta el borde.
            val deg = -135f + animated / MAX_SPEED * 270f
            rotate(deg, c) {
                val topLeft = Offset(c.x - 7f * u, c.y - 196f * u)
                val needle = Size(14f * u, 96f * u)
                val r = CornerRadius(5f * u)
                drawRoundRect(NeedleRed, topLeft, needle, r)
                drawRoundRect(Color.Black, topLeft, needle, r, style = Stroke(2.5f * u))
            }

            // Círculo central negro con aro naranja.
            drawCircle(Color.Black, 104f * u, c)
            drawCircle(p.accent, 104f * u, c, style = Stroke(3f * u))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                shown.roundToInt().toString(),
                color = p.accent, fontSize = 82.sp, lineHeight = 74.sp,
                fontFamily = Tema.condensed, fontWeight = FontWeight.Bold,
            )
            Text(
                "km/h", color = p.muted, fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                fontFamily = Tema.body, modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
