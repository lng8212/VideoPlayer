package com.longkd.videoplayer.utils

import android.graphics.Rect
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.longkd.videoplayer.interfaces.VideoPlayer
import com.longkd.videoplayer.model.VideoMessage
import com.longkd.videoplayer.ui.VideoViewHolder

class VideoScrollDetector(
    private val recyclerView: RecyclerView,
    private val videoPlayer: VideoPlayer,
    private val videoMessages: List<VideoMessage>
) {

    fun playClickVideo(position: Int) {
        val holder =
            recyclerView.findViewHolderForAdapterPosition(position) as? VideoViewHolder ?: return
        val rect = Rect()
        if (holder.itemView.getGlobalVisibleRect(rect)) {
            // This item is visible — play it
            val video = videoMessages.getOrNull(position) ?: return
            videoPlayer.prepareAndPlay(
                position,
                video.assetPath,
                holder.textureView,
                holder.thumbnailView
            )
        }
    }

    fun detectAndPlayVisibleVideo() {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        val lastVisible = layoutManager.findLastVisibleItemPosition()

        if (firstVisible == RecyclerView.NO_POSITION || lastVisible == RecyclerView.NO_POSITION) return

        for (i in lastVisible - 1 downTo firstVisible) {
            val holder =
                recyclerView.findViewHolderForAdapterPosition(i) as? VideoViewHolder ?: continue
            val rect = Rect()
            if (holder.itemView.getGlobalVisibleRect(rect)) {
                // This item is visible — play it
                val video = videoMessages.getOrNull(i) ?: continue
                videoPlayer.prepareAndPlay(
                    i,
                    video.assetPath,
                    holder.textureView,
                    holder.thumbnailView
                )
                break // Only play the first one found from bottom up
            }
        }
    }
}