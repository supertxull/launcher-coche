package com.radiocoche.launcher.radar

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import com.radiocoche.launcher.Pkg
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/**
 * Pitidos, silencio y destellos.
 *
 * Silencio: mientras suenan los pitidos la app pide el sonido en exclusiva (las apps de música se paran)
 * y avisa a la Radio para que se calle; al acabar el último pitido lo devuelve todo.
 */
class AlertRunner(private val ctx: Context) {

    private val main = Handler(Looper.getMainLooper())
    private val audio = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val overlay = FlashOverlay(ctx)
    private var focus: AudioFocusRequest? = null
    private var muted = false
    private val tone: ShortArray = makeTone()

    /** Hay destellos en curso. */
    var flashing = false
        private set

    private val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    /** Aviso al acercarse: silencio, dos pitidos con dos destellos y vuelve el sonido. */
    fun approach(alert: Alert, count: Int = 2) {
        main.removeCallbacksAndMessages(null)
        flashing = true
        mute()
        val useOverlay = Settings.canDrawOverlays(ctx)
        RadarState.overlayActive = useOverlay
        var t = 150L
        for (i in 0 until count) {
            main.postDelayed({
                beep()
                RadarState.flash = true
                if (useOverlay) overlay.show(alert)
            }, t)
            main.postDelayed({
                RadarState.flash = false
                if (useOverlay) overlay.hide()
            }, t + 420)
            if (i == count - 1) main.postDelayed({ unmute() }, t + 260)
            t += 680
        }
        main.postDelayed({
            flashing = false
            overlay.remove()
            RadarState.overlayActive = false
        }, t)
    }

    /** Salida de un tramo: un pitido. */
    fun sectionEnd() {
        mute()
        main.postDelayed({ beep() }, 150)
        main.postDelayed({ unmute() }, 410)
    }

    fun release() {
        main.removeCallbacksAndMessages(null)
        overlay.remove()
        if (muted) unmute()
        RadarState.flash = false
    }

    // ------------------------------------------------------------------ sonido

    private fun mute() {
        if (muted) return
        muted = true
        if (Build.VERSION.SDK_INT >= 26) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attrs)
                .setOnAudioFocusChangeListener { }
                .build()
            focus = req
            runCatching { audio.requestAudioFocus(req) }
        } else {
            @Suppress("DEPRECATION")
            runCatching { audio.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT) }
        }
        radio("mute")
    }

    private fun unmute() {
        if (!muted) return
        muted = false
        if (Build.VERSION.SDK_INT >= 26) {
            focus?.let { runCatching { audio.abandonAudioFocusRequest(it) } }
            focus = null
        } else {
            @Suppress("DEPRECATION")
            runCatching { audio.abandonAudioFocus(null) }
        }
        radio("unmute")
    }

    /** Orden a la Radio propia (desde la Radio 0.20), solo si lo último que se usó es la Radio. */
    private fun radio(cmd: String) {
        val source = ctx.getSharedPreferences("launcher", Context.MODE_PRIVATE).getString("source", "")
        if (source != "RADIO") return
        runCatching {
            ctx.sendBroadcast(Intent("com.radiocoche.radio.CMD").setPackage(Pkg.RADIO).putExtra("cmd", cmd))
        }
    }

    private fun beep() {
        runCatching {
            val track = AudioTrack.Builder()
                .setAudioAttributes(attrs)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(tone.size * 2)
                .build()
            track.write(tone, 0, tone.size)
            track.play()
            main.postDelayed({ runCatching { track.stop(); track.release() } }, 600)
        }
    }

    private fun makeTone(): ShortArray {
        val n = RATE * 190 / 1000
        val fade = RATE * 8 / 1000
        return ShortArray(n) { i ->
            val env = min(1.0, min(i, n - 1 - i).toDouble() / fade)
            (sin(2 * PI * 1250.0 * i / RATE) * env * 0.85 * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private companion object {
        const val RATE = 44100
    }
}
