package com.radiocoche.launcher.radar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager

/**
 * Destello a pantalla completa por encima de cualquier app (Maps, Waze, Android Auto…).
 * Necesita el permiso «Mostrar sobre otras apps»; sin él, el destello solo se ve en el launcher.
 */
class FlashOverlay(private val ctx: Context) {

    private val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var view: FlashView? = null

    fun show(alert: Alert) {
        val v = view ?: FlashView(ctx).also { nv ->
            val type = windowType()
            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.OPAQUE,
            )
            lp.gravity = Gravity.TOP or Gravity.START
            if (runCatching { wm.addView(nv, lp) }.isFailure) return
            view = nv
        }
        v.alert = alert
        v.visibility = View.VISIBLE
        v.invalidate()
    }

    fun hide() {
        view?.visibility = View.INVISIBLE
    }

    fun remove() {
        view?.let { runCatching { wm.removeView(it) } }
        view = null
    }

    private class FlashView(ctx: Context) : View(ctx) {
        var alert: Alert? = null

        private val condensed: Typeface = runCatching {
            Typeface.createFromAsset(ctx.assets, "fonts/BarlowCondensed-Bold.ttf")
        }.getOrElse { Typeface.create("sans-serif-condensed", Typeface.BOLD) }
        private val body: Typeface = runCatching {
            Typeface.createFromAsset(ctx.assets, "fonts/Barlow-SemiBold.ttf")
        }.getOrElse { Typeface.DEFAULT_BOLD }

        private val p = Paint(Paint.ANTI_ALIAS_FLAG)

        override fun onDraw(c: Canvas) {
            val a = alert ?: return
            val w = width.toFloat()
            val h = height.toFloat()
            // La pantalla del diseño mide 1024×552: se escala igual que el launcher.
            val u = minOf(w / 1024f, h / 552f)
            c.drawColor(Color.BLACK)

            // Señal
            val r = 165 * u
            val cx = w / 2 - 210 * u
            val cy = h / 2
            p.style = Paint.Style.FILL
            p.color = Color.WHITE
            c.drawCircle(cx, cy, r, p)
            p.style = Paint.Style.STROKE
            p.color = RED
            p.strokeWidth = 40 * u
            c.drawCircle(cx, cy, r - 20 * u, p)
            p.style = Paint.Style.FILL
            if (a.kind == Kind.RED_LIGHT) {
                drawLight(c, cx, cy, u)
            } else {
                p.color = Color.BLACK
                p.typeface = condensed
                p.textAlign = Paint.Align.CENTER
                p.textSize = 150 * u
                val txt = a.limit?.toString() ?: "!"
                c.drawText(txt, cx, cy - (p.ascent() + p.descent()) / 2, p)
            }

            // Textos
            val x = cx + r + 56 * u
            p.textAlign = Paint.Align.LEFT
            p.typeface = condensed
            p.color = ORANGE
            p.textSize = 62 * u
            val title = if (a.kind == Kind.SECTION) "INICIO DE TRAMO" else a.kind.label.uppercase()
            c.drawText(title, x, cy - 40 * u, p)
            p.color = Color.WHITE
            p.textSize = 54 * u
            val sub = when {
                a.kind == Kind.SECTION -> "Límite medio ${a.limit ?: "—"}"
                a.kind == Kind.RED_LIGHT -> "Semáforo con cámara"
                else -> "a ${roundDistance(a.distance)} m"
            }
            c.drawText(sub, x, cy + 28 * u, p)
            if (a.kind != Kind.RED_LIGHT) {
                p.typeface = body
                p.textSize = 26 * u
                val over = a.limit != null && a.speed > a.limit
                p.color = if (over) Color.rgb(0xFF, 0x40, 0x40) else Color.rgb(0xDC, 0xE2, 0xEA)
                c.drawText("Vas a ${a.speed} km/h", x, cy + 84 * u, p)
            }
        }

        private fun drawLight(c: Canvas, cx: Float, cy: Float, u: Float) {
            p.color = Color.rgb(0x1A, 0x1A, 0x1A)
            c.drawRoundRect(RectF(cx - 40 * u, cy - 82 * u, cx + 40 * u, cy + 82 * u), 18 * u, 18 * u, p)
            p.color = RED; c.drawCircle(cx, cy - 50 * u, 19 * u, p)
            p.color = Color.rgb(0x5A, 0x4A, 0x10); c.drawCircle(cx, cy, 19 * u, p)
            p.color = Color.rgb(0x16, 0x46, 0x1F); c.drawCircle(cx, cy + 50 * u, 19 * u, p)
        }
    }

    companion object {
        private val RED = Color.rgb(0xFF, 0x00, 0x00)
        private val ORANGE = Color.rgb(0xFF, 0x6A, 0x1A)

        /** Distancia redondeada a 10 m. */
        fun roundDistance(m: Int): Int = (m + 5) / 10 * 10

        @Suppress("DEPRECATION")
        private fun windowType(): Int =
            if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE
    }
}
