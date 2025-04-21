package com.longkd.videoplayer.player

import android.content.Context
import android.graphics.SurfaceTexture
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.ImageView
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.AssetDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.recyclerview.widget.RecyclerView
import com.longkd.videoplayer.interfaces.VideoPlayer

@OptIn(UnstableApi::class)
class VideoPlayerManager(private val context: Context) : VideoPlayer {

    private var player: ExoPlayer? = null
    override var currentPlayingPosition: Int = RecyclerView.NO_POSITION
        private set
    private val assetFactory = DataSource.Factory { AssetDataSource(context) }
    private var currentTextureView: TextureView? = null
    private var currentThumbnailView: ImageView? = null

    private fun getPlayer(): ExoPlayer {
        if (player == null) {
            player = ExoPlayer.Builder(context).build().apply {
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_OFF

                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        Log.d("ExoPlayer", "Playback state: $state")
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        Log.e("ExoPlayer", "Player error: ${error.message}")
                    }
                })
            }
        }
        return player!!
    }

    override fun prepareAndPlay(
        position: Int,
        assetPath: String,
        textureView: TextureView,
        thumbnailView: ImageView
    ) {
        val player = getPlayer()

        // Ignore if already playing same position
        if (position == currentPlayingPosition && textureView == currentTextureView) return

        // Stop previous video
        player.stop()
        player.clearMediaItems()
        player.clearVideoSurface()

        // Show thumbnail and hide texture for old item
        currentTextureView?.visibility = View.INVISIBLE
        currentThumbnailView?.visibility = View.VISIBLE

        // Update current views
        currentPlayingPosition = position
        currentTextureView = textureView
        currentThumbnailView = thumbnailView

        // Prepare views
        textureView.visibility = View.VISIBLE
        thumbnailView.visibility = View.INVISIBLE

        val mediaItem = MediaItem.fromUri("asset:///$assetPath".toUri())
        val source = ProgressiveMediaSource.Factory(assetFactory).createMediaSource(mediaItem)

        player.setMediaSource(source)
        player.prepare()

        setupSurfaceTexture(textureView)
    }

    private fun setupSurfaceTexture(textureView: TextureView) {
        fun attachSurface(surface: Surface) {
            player?.setVideoSurface(surface)
            player?.playWhenReady = true
        }

        if (textureView.isAvailable) {
            attachSurface(Surface(textureView.surfaceTexture))
        } else {
            textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(
                    surface: SurfaceTexture,
                    width: Int,
                    height: Int
                ) {
                    attachSurface(Surface(surface))
                }

                override fun onSurfaceTextureSizeChanged(
                    surface: SurfaceTexture,
                    width: Int,
                    height: Int
                ) {
                }

                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true
                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
            }
        }
    }

    override fun pause() {
        player?.pause()
    }

    override fun release() {
        player?.release()
        player = null
        currentTextureView = null
        currentThumbnailView = null
    }
}