package com.longkd.videoplayer.cache

import android.graphics.Bitmap
import android.util.LruCache

class ThumbnailCache {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()

    private val cacheSize = maxMemory / 8

    private val bitmapCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }

        override fun entryRemoved(
            evicted: Boolean,
            key: String,
            oldValue: Bitmap,
            newValue: Bitmap?
        ) {
            if (evicted && newValue == null && !oldValue.isRecycled) {
                oldValue.recycle()
            }
        }
    }

    fun get(key: String): Bitmap? = bitmapCache.get(key)

    fun put(key: String, bitmap: Bitmap) {
        bitmapCache.put(key, bitmap)
    }
}
