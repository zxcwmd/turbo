package com.omnisound.app.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

/** Thin Media3 wrapper. Supply only stream URLs returned by SoundCloud's API. */
class PlaybackController(context: Context) {
    private val player = ExoPlayer.Builder(context).build()
    fun play(streamUrl: String) {
        player.setMediaItem(MediaItem.fromUri(streamUrl))
        player.prepare()
        player.play()
    }
    fun pause() = player.pause()
    fun resume() = player.play()
    fun seekTo(positionMs: Long) = player.seekTo(positionMs)
    fun release() = player.release()
}
