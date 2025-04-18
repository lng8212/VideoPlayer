package com.longkd.videoplayer

import android.graphics.SurfaceTexture
import android.os.Bundle
import android.view.Surface
import android.view.TextureView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.longkd.videoplayer.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var player: ExoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize ExoPlayer
        player = ExoPlayer.Builder(this)
            .build()

        binding.textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            @UnstableApi
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                val surfaceObj = Surface(surface)
                player.setVideoSurface(surfaceObj)
                preparePlayer()
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                player.setVideoSurface(null)
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }
    }

    @UnstableApi
    private fun preparePlayer() {
        val mediaItem =
            MediaItem.fromUri("asset:///sample-5s.mp4") // Change to your video URI
        val mediaSource = buildMediaSource(mediaItem)
        player.setMediaSource(mediaSource)
        player.prepare()
        player.play()
    }

    @UnstableApi
    private fun buildMediaSource(mediaItem: MediaItem): ProgressiveMediaSource {
        val dataSourceFactory: DataSource.Factory = DefaultDataSource.Factory(this)
        return ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(mediaItem)
    }


    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}