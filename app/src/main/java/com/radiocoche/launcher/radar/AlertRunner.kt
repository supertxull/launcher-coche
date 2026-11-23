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
import com.radiocoche.launcher.ui.CarLights
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
        // Un solo aviso grande, fijo mientras suenan los pitidos (dura lo mismo que los dos destellos de antes).
        var t = 150L
        main.postDelayed({
            RadarState.flash = true
            if (useOverlay) overlay.show(alert)
        }, t)
        for (i in 0 until count) {
            main.postDelayed({ beep() }, t)
            if (i == count - 1) {
                main.postDelayed({ unmute() }, t + 260)
                main.postDelayed({
                    RadarState.flash = false
                    if (useOverlay) overlay.hide()
                }, t + 420)
            }
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
        silenceMusic(true)
        loud()
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
        restoreVolume()
        silenceMusic(false)
    }

    // ------------------------------------------------------------------ volumen y Bluetooth

    /** Volumen general que había antes del aviso (para devolverlo); null = no se tocó. */
    private var savedVolume: Int? = null

    /**
     * Pone el volumen general de la unidad (el del mando, 0–30) en el nivel elegido para los avisos,
     * para que se oigan aunque el mando esté a 0. Orden del microcontrolador: (0x203, 1, nivel).
     */
    private fun loud() {
        val now = CarLights.volume ?: return // sin saber el volumen actual no se toca (no se podría devolver)
        val target = AlertVolume.get(ctx)
        if (now == target) return
        if (CarLights.write(MSG_VOLUME, 1, target)) savedVolume = now
    }

    private fun restoreVolume() {
        val v = savedVolume ?: return
        savedVolume = null
        CarLights.write(MSG_VOLUME, 1, v)
    }

    /**
     * Silencia la música mientras suena el aviso (en esta unidad el foco de sonido de Android no la para):
     *  - Música (nuestra app): orden "com.radiocoche.musica.CMD" mute/unmute (baja su volumen a 0, sigue avanzando).
     *  - Música por Bluetooth (app de fábrica com.tw.bt): orden "com.tw.launcher.btmsg" msg 11 = silenciar, 12 = quitar silencio.
     * Si la app no está en marcha la orden no hace nada.
     */
    private var radioMuted = false

    /** Silencio sin pitido ni cambio de volumen (llamadas del Teléfono). */
    fun silence(on: Boolean) {
        if (on) {
            radio("mute")
            silenceMusic(true)
        } else {
            silenceMusic(false)
            radio("unmute")
        }
    }

    private fun silenceMusic(on: Boolean) {
        runCatching {
            ctx.sendBroadcast(Intent("com.radiocoche.musica.CMD").setPackage("com.radiocoche.musica").putExtra("cmd", if (on) "mute" else "unmute"))
        }
        runCatching {
            ctx.sendBroadcast(Intent("com.tw.launcher.btmsg").setPackage("com.tw.bt").putExtra("msg", if (on) 11 else 12))
        }
    }

    /**
     * Quita (mute) o devuelve (unmute) el sonido de la radio, solo si lo último que se usó es la Radio.
     *
     * Se hace desde el launcher con su conexión al microcontrolador, que ya está abierta: así es inmediato.
     * Antes se pedía a la app Radio, que tenía que arrancar, abrir la conexión, pedir el estado, escribir
     * y cerrarla enseguida: el sonido tardaba en volver.
     */
    private fun radio(cmd: String) {
        // Solo si la radio es la fuente que suena ahora (si no, quitarle el silencio la haría volver).
        val src = CarLights.source
        if (src != null) {
            if (src != SRC_RADIO && !radioMuted) return
        } else {
            val last = ctx.getSharedPreferences("launcher", Context.MODE_PRIVATE).getString("source", "")
            if (last != "RADIO" && !radioMuted) return
        }
        if (cmd == "mute") radioMuted = true else if (!radioMuted) return else radioMuted = false
        val on = cmd == "unmute"
        if (CarLights.write(MSG_SOURCE, RADIO_AUDIO, if (on) 1 else 0)) return
        // Sin conexión propia: se lo pide a la Radio (Radio 0.20+).
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
        /** Fuente de audio del microcontrolador (la Radio usa (0x9E11, 192, 1) para pedir su audio). */
        const val MSG_SOURCE = 0x9E11
        const val RADIO_AUDIO = 192
        /** Volumen general de la unidad: (0x203, 1, nivel 0–30). */
        const val MSG_VOLUME = 0x203
        const val SRC_RADIO = 1
    }
}
