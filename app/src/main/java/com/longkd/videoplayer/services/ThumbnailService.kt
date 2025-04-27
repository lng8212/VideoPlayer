package com.longkd.videoplayer.services

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
import androidx.core.graphics.scale
import com.longkd.videoplayer.cache.ThumbnailCache
import com.longkd.videoplayer.interfaces.ThumbnailProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ThumbnailService(
    private val context: Context,
    private val thumbnailCache: ThumbnailCache
) : ThumbnailProvider {

    override fun getCachedThumbnail(assetPath: String): Bitmap? {
        return thumbnailCache.get(assetPath)
    }

    override fun cacheThumbnail(assetPath: String, bitmap: Bitmap) {
        thumbnailCache.put(assetPath, bitmap)
    }

    override suspend fun generateThumbnail(assetPath: String): Bitmap? =
        withContext(Dispatchers.IO) {
            var retriever: MediaMetadataRetriever? = null
            var afd: AssetFileDescriptor? = null

            try {
                retriever = MediaMetadataRetriever()
                afd = context.assets.openFd(assetPath)
                retriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)

                val original =
                    retriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST)
                return@withContext original?.let {
                    val width = 280
                    val height = 200
                    val scaled = it.scale(width, height)

                    if (scaled != it) {
                        it.recycle()
                    }

                    scaled
                }
            } catch (e: Exception) {
                Log.e("ThumbnailService", "Thumbnail generation failed: ${e.message}")
                return@withContext null
            } finally {
                try {
                    retriever?.release()
                    afd?.close()
                } catch (e: Exception) {
                    Log.e("ThumbnailService", "Error releasing resources: ${e.message}")
                }
            }
        }
}
