package com.longkd.videoplayer.interfaces

import android.graphics.Bitmap

interface ThumbnailProvider {
    suspend fun generateThumbnail(assetPath: String): Bitmap?
    fun getCachedThumbnail(assetPath: String): Bitmap?
    fun cacheThumbnail(assetPath: String, bitmap: Bitmap)
}