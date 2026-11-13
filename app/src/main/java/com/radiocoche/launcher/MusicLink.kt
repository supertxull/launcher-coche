package com.radiocoche.launcher

import android.content.ComponentName
import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture

/** Lo que suena en la app Música y sus mandos (anterior, pausa, siguiente). */
class MusicLink(private val ctx: Context) {

    var connected by mutableStateOf(false)
        private set
    var title by mutableStateOf("")
        private set
    var artist by mutableStateOf("")
        private set
    var album by mutableStateOf("")
        private set
    var art by mutableStateOf<ImageBitmap?>(null)
        private set
    var playing by mutableStateOf(false)
        private set
    var positionMs by mutableStateOf(0L)
        private set
    var durationMs by mutableStateOf(0L)
        private set

    /** Se llama cuando la música empieza a sonar. */
    var onStartedPlaying: (() -> Unit)? = null

    private var future: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var artOf: MediaMetadata? = null

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = sync()
    }

    private val controllerListener = object : MediaController.Listener {
        override fun onDisconnected(controller: MediaController) = release()
    }

    fun connect() {
        if (controller != null || future != null) return
        if (ctx.packageManager.getLaunchIntentForPackage(Pkg.MUSICA) == null) return
        runCatching {
            val token = SessionToken(ctx, ComponentName(Pkg.MUSICA, Pkg.MUSICA + ".playback.PlaybackService"))
            val f = MediaController.Builder(ctx, token).setListener(controllerListener).buildAsync()
            future = f
            f.addListener({
                try {
                    val c = f.get()
                    controller = c
                    connected = true
                    c.addListener(playerListener)
                    sync()
                } catch (e: Exception) {
                    future = null
                }
            }, ContextCompat.getMainExecutor(ctx))
        }
    }

    fun release() {
        controller?.removeListener(playerListener)
        future?.let { runCatching { MediaController.releaseFuture(it) } }
        future = null
        controller = null
        connected = false
        playing = false
    }

    /** Actualiza la barra de progreso. */
    fun tick() {
        val c = controller ?: return
        positionMs = c.currentPosition.coerceAtLeast(0)
    }

    fun playPause(): Boolean {
        val c = controller ?: return false
        if (c.isPlaying) c.pause() else c.play()
        return true
    }

    fun next(): Boolean {
        val c = controller ?: return false
        c.seekToNext()
        return true
    }

    fun previous(): Boolean {
        val c = controller ?: return false
        c.seekToPrevious()
        return true
    }

    private fun sync() {
        val c = controller ?: return
        val md = c.mediaMetadata
        title = md.title?.toString() ?: md.displayTitle?.toString() ?: ""
        artist = md.artist?.toString() ?: md.albumArtist?.toString() ?: ""
        album = md.albumTitle?.toString() ?: ""
        if (md !== artOf) {
            artOf = md
            art = loadArt(md)
        }
        val d = c.duration
        durationMs = if (d == C.TIME_UNSET || d < 0) 0 else d
        positionMs = c.currentPosition.coerceAtLeast(0)
        val now = c.isPlaying
        if (now && !playing) onStartedPlaying?.invoke()
        playing = now
    }

    private fun loadArt(md: MediaMetadata): ImageBitmap? = runCatching {
        val data = md.artworkData
        val bmp = when {
            data != null -> BitmapFactory.decodeByteArray(data, 0, data.size)
            md.artworkUri != null -> ctx.contentResolver.openInputStream(md.artworkUri!!)?.use { BitmapFactory.decodeStream(it) }
            else -> null
        }
        bmp?.asImageBitmap()
    }.getOrNull()
}
