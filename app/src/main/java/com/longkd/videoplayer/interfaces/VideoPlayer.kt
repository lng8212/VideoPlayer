package com.longkd.videoplayer.interfaces

import com.longkd.videoplayer.ui.custom.VideoPlayerView

interface VideoPlayer {
    fun prepareAndPlay(
        position: Int,
        assetPath: String,
        videoPlayerView: VideoPlayerView
    )

    fun pause()
    fun release()
    val currentPlayingPosition: Int
}