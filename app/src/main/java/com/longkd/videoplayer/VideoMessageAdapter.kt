package com.longkd.videoplayer


import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import com.longkd.videoplayer.model.VideoMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(UnstableApi::class)
class VideoMessageAdapter(
    private val videos: List<VideoMessage>,
    private val onItemClick: (position: Int) -> Unit
) : RecyclerView.Adapter<VideoViewHolder>() {

    override fun getItemViewType(position: Int): Int {
        return if (videos[position].isSender) 0 else 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val layout = if (viewType == 0) R.layout.item_video_sent else R.layout.item_video_received
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return VideoViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videos[position]
        holder.bind(video, position)
    }

    override fun getItemCount(): Int = videos.size

    override fun onViewRecycled(holder: VideoViewHolder) {
        super.onViewRecycled(holder)
        holder.onViewRecycled()
    }
}


class VideoViewHolder(itemView: View, private val onItemClick: (position: Int) -> Unit) :
    RecyclerView.ViewHolder(itemView) {
    val textureView: TextureView = itemView.findViewById(R.id.texture_view)
    val thumbnailView: ImageView = itemView.findViewById(R.id.thumbnail_view)
    private var thumbnailJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun bind(video: VideoMessage, position: Int) {
        // Cancel any previous job
        thumbnailJob?.cancel()

        // Tag the view with the asset path for reference
        itemView.tag = video.assetPath

        thumbnailView.setOnClickListener {
            onItemClick.invoke(position)
        }
        // Always show thumbnail view initially
        showThumbnail()

        // Set placeholder while loading
        thumbnailView.setImageResource(R.drawable.ic_launcher_background)

        // Check cache first
        val cacheKey = video.assetPath
        val cachedBitmap = ThumbnailCache.bitmapCache.get(cacheKey)

        if (cachedBitmap != null && !cachedBitmap.isRecycled) {
            thumbnailView.setImageBitmap(cachedBitmap)
            return
        }

        // Load thumbnail in background with coroutines
        thumbnailJob = scope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                generateLowResolutionThumbnail(itemView.context, video.assetPath)
            }

            if (isActive && bitmap != null && !bitmap.isRecycled) {
                // Store in cache first (important to do this before setting the image)
                ThumbnailCache.bitmapCache.put(cacheKey, bitmap)
                // Then set the image
                thumbnailView.setImageBitmap(bitmap)
            }
        }
    }

    fun showThumbnail() {
        thumbnailView.visibility = View.VISIBLE
        textureView.visibility = View.INVISIBLE
    }

    // Generate a low resolution thumbnail for better performance
    private fun generateLowResolutionThumbnail(context: Context, assetPath: String): Bitmap? {
        var retriever: MediaMetadataRetriever? = null
        var afd: AssetFileDescriptor? = null

        try {
            retriever = MediaMetadataRetriever()
            afd = context.assets.openFd(assetPath)
            retriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)

            // Get a frame at 1 second
            val original =
                retriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST)
            return original?.let {
                // Scale down to thumbnail size
                val width = 200
                val height = 112
                val scaled = Bitmap.createScaledBitmap(it, width, height, true)

                // If we created a new bitmap, recycle the original
                if (scaled != it) {
                    it.recycle()
                }

                scaled
            }
        } catch (e: Exception) {
            Log.e("VideoViewHolder", "Thumbnail generation failed: ${e.message}")
            return null
        } finally {
            // Clean up resources manually
            try {
                retriever?.release()
                afd?.close()
            } catch (e: Exception) {
                Log.e("VideoViewHolder", "Error releasing resources: ${e.message}")
            }
        }
    }

    // Make sure to cancel any pending jobs when the view is recycled
    fun onViewRecycled() {
        thumbnailJob?.cancel()

        // DON'T recycle bitmaps in the cache
        thumbnailView.setImageDrawable(null)
    }
}


// 5. Data model for video messages
data class VideoMessage(
    val id: String,
    val assetPath: String,
    val isSender: Boolean
)