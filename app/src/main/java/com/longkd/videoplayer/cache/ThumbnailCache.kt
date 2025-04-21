package com.longkd.videoplayer.cache

import android.graphics.Bitmap
import android.util.LruCache

class ThumbnailCache {
    // Calculate the max memory the app can use
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()

    // Use 1/8th of the available memory for this memory cache
    private val cacheSize = maxMemory / 8

    private val bitmapCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            // The cache size will be measured in kilobytes
            return bitmap.byteCount / 1024
        }

        // Override entryRemoved to handle removal of bitmaps from cache
        override fun entryRemoved(
            evicted: Boolean,
            key: String,
            oldValue: Bitmap,
            newValue: Bitmap?
        ) {
            // Only recycle if it's evicted due to cache size limitations
            // and when it's not being replaced by a new bitmap
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
