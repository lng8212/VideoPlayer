package com.longkd.videoplayer.ui

import android.view.View
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
    private val onPlayItemClick: (position: Int) -> Unit,
    private val onFullScreenClick: (position: Int, transitionName: String) -> Unit
) : RecyclerView.ViewHolder(itemView) {

    val videoPlayerView: VideoPlayerView = itemView.findViewById(R.id.video_player_view)
    private var thumbnailJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun bind(video: VideoMessage, position: Int) {
        thumbnailJob?.cancel()

        videoPlayerView.setOnToggleFullscreenListener {
            onFullScreenClick.invoke(position, videoPlayerView.textureView.transitionName)
        }

        itemView.tag = video.assetPath

        videoPlayerView.textureView.transitionName = video.assetPath

        videoPlayerView.thumbnailView.setOnClickListener {
            onPlayItemClick.invoke(position)
        }

        showThumbnail()
        videoPlayerView.thumbnailView.setImageResource(R.drawable.ic_launcher_background)

        val cacheKey = video.assetPath
        val cachedBitmap = thumbnailProvider.getCachedThumbnail(cacheKey)

        if (cachedBitmap != null && !cachedBitmap.isRecycled) {
            videoPlayerView.thumbnailView.setImageBitmap(cachedBitmap)
            return
        }

        thumbnailJob = scope.launch {
            val bitmap = thumbnailProvider.generateThumbnail(video.assetPath)

            if (isActive && bitmap != null && !bitmap.isRecycled) {
                thumbnailProvider.cacheThumbnail(cacheKey, bitmap)
                videoPlayerView.thumbnailView.setImageBitmap(bitmap)
            }
        }
    }

    fun showThumbnail() {
        videoPlayerView.thumbnailView.visibility = View.VISIBLE
        videoPlayerView.textureView.visibility = View.INVISIBLE
    }

    fun onViewRecycled() {
        thumbnailJob?.cancel()
        scope.cancel()
        videoPlayerView.thumbnailView.setImageDrawable(null)
    }
}