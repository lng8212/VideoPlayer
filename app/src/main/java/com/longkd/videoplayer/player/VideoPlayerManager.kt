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
import com.longkd.videoplayer.ui.custom.VideoPlayerView
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(UnstableApi::class)
@Singleton
class VideoPlayerManager @Inject constructor(@ApplicationContext private val context: Context) :
    VideoPlayer {

    override var currentPlayingPosition: Int = RecyclerView.NO_POSITION
        private set
    private val assetFactory = DataSource.Factory { AssetDataSource(context) }
    private var currentTextureView: TextureView? = null
    private var currentThumbnailView: ImageView? = null
    private var savedSurfaceTexture: SurfaceTexture? = null
    private var originalPlayerView: VideoPlayerView? = null


    private val player: ExoPlayer by lazy {
        ExoPlayer.Builder(context).build().apply {
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

    override fun prepareAndPlay(
        position: Int,
        assetPath: String,
        videoPlayerView: VideoPlayerView
    ) {

        if (position == currentPlayingPosition && videoPlayerView.textureView == currentTextureView) return
        originalPlayerView = videoPlayerView
        player.stop()
        player.clearMediaItems()
        player.clearVideoSurface()

        currentTextureView?.visibility = View.INVISIBLE
        currentThumbnailView?.visibility = View.VISIBLE

        currentPlayingPosition = position
        currentTextureView = videoPlayerView.textureView
        currentThumbnailView = videoPlayerView.thumbnailView

        videoPlayerView.textureView.visibility = View.VISIBLE

        val mediaItem = MediaItem.fromUri("asset:///$assetPath".toUri())
        val source = ProgressiveMediaSource.Factory(assetFactory).createMediaSource(mediaItem)

        player.addListener(object : Player.Listener {
            override fun onRenderedFirstFrame() {
                videoPlayerView.thumbnailView.visibility = View.INVISIBLE
                player.removeListener(this)
            }
        })

        player.setMediaSource(source)
        player.prepare()

        setupSurfaceTexture(videoPlayerView)
    }

    private fun setupSurfaceTexture(videoPlayerView: VideoPlayerView) {
        fun attachSurface(surface: Surface) {
            player.setVideoSurface(surface)
            player.playWhenReady = true
            player.let { videoPlayerView.bindPlayer(it) }
        }

        if (videoPlayerView.textureView.isAvailable) {
            attachSurface(Surface(videoPlayerView.textureView.surfaceTexture))
        } else {
            videoPlayerView.textureView.surfaceTextureListener =
                object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(
                        surface: SurfaceTexture,
                        width: Int,
                        height: Int
                    ) {
                        savedSurfaceTexture = currentTextureView?.surfaceTexture
                        attachSurface(Surface(surface))
                        Log.d("xxxxx", "onSurfaceTextureAvailable: $width $height")
                    }

                    override fun onSurfaceTextureSizeChanged(
                        surface: SurfaceTexture,
                        width: Int,
                        height: Int
                    ) {
                        Log.d("xxxxx", "onSurfaceTextureSizeChanged: $width $height")
                    }

                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                        player.clearVideoSurface()
                        return true
                    }

                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                        Log.d("xxxxx", "onSurfaceTextureUpdated: ")
                    }
                }
        }
    }

    fun attachToFullscreen(videoPlayerView: VideoPlayerView) {
        val currentPosition = player.currentPosition
        val wasPlaying = player.isPlaying

        // Clear previous surface
        player.clearVideoSurface()

        // Update references
        currentTextureView = videoPlayerView.textureView
        currentThumbnailView = videoPlayerView.thumbnailView

        // Setup new surface
        setupSurfaceTexture(videoPlayerView)

        // Restore playback state
        player.seekTo(currentPosition)
        if (wasPlaying) {
            player.play()
        }
    }


    fun detachFromFullscreen() {
        val currentPosition = player.currentPosition
        val wasPlaying = player.isPlaying

        player.clearVideoSurface()
        originalPlayerView?.let {
            currentTextureView = it.textureView
            currentThumbnailView = it.thumbnailView
            setupSurfaceTexture(it)
            player.seekTo(currentPosition)
            if (wasPlaying) {
                player.play()
            }
        }
    }


    override fun pause() {
        player.pause()
    }

    override fun release() {
        // Instead of fully releasing, just pause and reset
        player.pause()
        player.stop()
        player.clearMediaItems()
        player.clearVideoSurface()
        currentTextureView = null
        currentThumbnailView = null
    }

    fun releaseCompletely() {
        player.release()
        currentTextureView = null
        currentThumbnailView = null
    }

}