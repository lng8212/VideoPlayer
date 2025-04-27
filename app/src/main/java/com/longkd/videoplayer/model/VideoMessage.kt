package com.longkd.videoplayer.model

data class VideoMessage(
    val id: String,
    val assetPath: String,
    val isSender: Boolean
)