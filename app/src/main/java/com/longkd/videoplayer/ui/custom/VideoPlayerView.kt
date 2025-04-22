package com.longkd.videoplayer.ui.custom

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.SeekBar
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.longkd.videoplayer.R
import com.longkd.videoplayer.databinding.ViewVideoPlayerBinding

open class VideoPlayerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding = ViewVideoPlayerBinding.inflate(LayoutInflater.from(context), this, true)
    var textureView = binding.textureView
    var thumbnailView = binding.thumbnailView

    private var onToggleFullscreen: (() -> Unit)? = null

    private var isFullScreen = false

    fun setOnToggleFullscreenListener(listener: () -> Unit) {
        onToggleFullscreen = listener
    }

    fun setIsFullScreen(isFullScreen: Boolean) {
        this.isFullScreen = isFullScreen
        if (isFullScreen) {
            binding.imgFullScreen.setImageResource(R.drawable.ic_exit_full_screen)
            binding.seekbar.visibility = VISIBLE
            binding.imgFullScreen.visibility = VISIBLE
        } else binding.imgFullScreen.setImageResource(R.drawable.ic_full_screen)
    }

    init {
        setOnClickListener {
            if (isShowController) hideController() else showController()
        }
        binding.imgFullScreen.setOnClickListener {
            onToggleFullscreen?.invoke()
        }
    }

    private var isShowController = true

    fun showController() {
        isShowController = true
        binding.clController.animate().alpha(1f).setDuration(300).start()
        binding.clController.visibility = VISIBLE
    }

    fun hideController() {
        isShowController = false
        binding.clController.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction { binding.clController.visibility = GONE }
            .start()
    }

    fun bindPlayer(player: ExoPlayer) {
        binding.btnPlayPause.setOnClickListener {
            if (player.isPlaying) {
                player.pause()
            } else {
                if (player.playbackState == Player.STATE_ENDED) {
                    player.seekTo(0)
                    player.play()
                } else {
                    player.play()
                }
            }
        }

        // Update SeekBar
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    binding.seekbar.visibility = VISIBLE
                    binding.imgFullScreen.visibility = VISIBLE
                    binding.seekbar.max = player.duration.toInt()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                if (isPlaying) binding.btnPlayPause.setImageResource(R.drawable.ic_pause)
                else binding.btnPlayPause.setImageResource(R.drawable.ic_play)
            }
        })

        // Poll seek position
        val handler = Handler(Looper.getMainLooper())
        val updateSeekBar = object : Runnable {
            override fun run() {
                if (player.isPlaying) {
                    binding.seekbar.progress = player.currentPosition.toInt()
                }
                handler.postDelayed(this, 500)
            }
        }
        handler.post(updateSeekBar)

        binding.seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    player.seekTo(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }
}