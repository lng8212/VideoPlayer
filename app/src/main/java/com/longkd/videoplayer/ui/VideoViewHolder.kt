package com.longkd.videoplayer.ui

import android.view.TextureView
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.longkd.videoplayer.R
import com.longkd.videoplayer.interfaces.ThumbnailProvider
import com.longkd.videoplayer.model.VideoMessage
import com.longkd.videoplayer.ui.custom.VideoPlayerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class VideoViewHolder(
    itemView: View,
    private val thumbnailProvider: ThumbnailProvider,
    private val onItemClick: (position: Int) -> Unit
) : RecyclerView.ViewHolder(itemView) {
    val textureView: TextureView = itemView.findViewById(R.id.texture_view)
    val thumbnailView: ImageView = itemView.findViewById(R.id.thumbnail_view)
    val videoPlayerView: VideoPlayerView = itemView.findViewById(R.id.video_player_view)
    private var thumbnailJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun bind(video: VideoMessage, position: Int) {
        thumbnailJob?.cancel()

        videoPlayerView.setOnToggleFullscreenListener {
            val fragment = FullscreenVideoDialogFragment()
            (itemView.context as AppCompatActivity)
                .supportFragmentManager
                .beginTransaction()
                .add(fragment, FullscreenVideoDialogFragment::class.simpleName)
                .commitAllowingStateLoss()
        }

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
        val cachedBitmap = thumbnailProvider.getCachedThumbnail(cacheKey)

        if (cachedBitmap != null && !cachedBitmap.isRecycled) {
            thumbnailView.setImageBitmap(cachedBitmap)
            return
        }

        // Load thumbnail in background with coroutines
        thumbnailJob = scope.launch {
            val bitmap = thumbnailProvider.generateThumbnail(video.assetPath)

            if (isActive && bitmap != null && !bitmap.isRecycled) {
                // Store in cache first
                thumbnailProvider.cacheThumbnail(cacheKey, bitmap)
                // Then set the image
                thumbnailView.setImageBitmap(bitmap)
            }
        }
    }

    fun showThumbnail() {
        thumbnailView.visibility = View.VISIBLE
        textureView.visibility = View.INVISIBLE
    }

    // Make sure to cancel any pending jobs when the view is recycled
    fun onViewRecycled() {
        thumbnailJob?.cancel()
        scope.cancel()
        thumbnailView.setImageDrawable(null)
    }
}