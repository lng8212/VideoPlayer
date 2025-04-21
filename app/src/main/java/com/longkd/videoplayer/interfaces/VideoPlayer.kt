package com.longkd.videoplayer.interfaces

import android.view.TextureView
import android.widget.ImageView

interface VideoPlayer {
    fun prepareAndPlay(
        position: Int,
        assetPath: String,
        textureView: TextureView,
        thumbnailView: ImageView
    )

    fun pause()
    fun release()
    val currentPlayingPosition: Int
}