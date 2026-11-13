package com.radiocoche.launcher.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val MAX_SPEED = 240f
private val Ink = Color(0xFF2A1003)
private val InkSoft = Color(0xFF3A1505)

/**
 * Velocímetro del diseño: franja naranja iluminada de 0 a 240 km/h, aguja roja
 * y la velocidad en cifras en el círculo central.
 */
@Composable
fun Speedometer(speedKmh: Float, modifier: Modifier) {
    val shown = speedKmh.coerceIn(0f, MAX_SPEED)
    val animated by animateFloatAsState(shown, tween(600), label = "aguja")
    val numbers = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            color = Ink.toArgb()
        }
    }
    val p = Tema.p

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            // El dibujo está pensado en una rejilla de 400×400.
            val u = size.minDimension / 400f
            val c = Offset(size.width / 2f, size.height / 2f)

            drawCircle(Color(0xFF120905), 198f * u, c)
            drawCircle(Color(0xFF2A1408), 198f * u, c, style = Stroke(3f * u))

            // Franja iluminada (abierta por abajo).
            val arcTopLeft = Offset(c.x - 160f * u, c.y - 160f * u)
            val arcSize = Size(320f * u, 320f * u)
            drawArc(Color(0x40FF8A1F), 125f, 290f, false, arcTopLeft, arcSize, style = Stroke(78f * u))
            drawArc(
                Brush.horizontalGradient(
                    listOf(Color(0xFFE8590C), Color(0xFFFFC24A), Color(0xFFE8590C)),
                    startX = c.x - 192f * u, endX = c.x + 192f * u,
                ),
                125f, 290f, false, arcTopLeft, arcSize, style = Stroke(64f * u),
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
                    if (major) Ink else InkSoft,
                    Offset(c.x + dx * r1, c.y + dy * r1),
                    Offset(c.x + dx * r2, c.y + dy * r2),
                    strokeWidth = (if (major) 3.2f else 1.8f) * u,
                )
            }

            // Números de 20 en 20, pegados a las rayitas.
            numbers.textSize = 24f * u
            val baseline = -(numbers.ascent() + numbers.descent()) / 2f
            for (i in 0..12) {
                val a = Math.toRadians(135.0 + i * 22.5)
                val x = c.x + cos(a).toFloat() * 155f * u
                val y = c.y + sin(a).toFloat() * 155f * u
                drawContext.canvas.nativeCanvas.drawText((i * 20).toString(), x, y + baseline, numbers)
            }

            // Aguja roja, con la misma estética que la marca del dial de la radio.
            val deg = -135f + animated / MAX_SPEED * 270f
            rotate(deg, c) {
                val topLeft = Offset(c.x - 3.5f * u, c.y - 194f * u)
                val needle = Size(7f * u, 88f * u)
                drawRoundRect(
                    NeedleRed.copy(alpha = 0.28f),
                    Offset(topLeft.x - 3f * u, topLeft.y - 2f * u),
                    Size(needle.width + 6f * u, needle.height + 4f * u),
                    CornerRadius(6.5f * u),
                )
                drawRoundRect(NeedleRed, topLeft, needle, CornerRadius(3.5f * u))
            }

            // Círculo central.
            drawCircle(p.bg, 112f * u, c)
            drawCircle(p.accent, 102f * u, c)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                shown.roundToInt().toString(),
                color = Color(0xFF111111), fontSize = 92.sp, lineHeight = 84.sp,
                fontFamily = Tema.condensed, fontWeight = FontWeight.Bold,
            )
            Text("km/h", color = Color(0xFF111111), fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
