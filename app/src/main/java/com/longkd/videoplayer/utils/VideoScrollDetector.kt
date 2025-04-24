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
            val video = videoMessages.getOrNull(position) ?: return
            videoPlayer.prepareAndPlay(
                position,
                video.assetPath,
                holder.videoPlayerView
            )
        }
    }

    fun detectAndPlayVisibleVideo(onGetPosition: (position: Int) -> Unit) {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        val lastVisible = layoutManager.findLastVisibleItemPosition()

        if (firstVisible == RecyclerView.NO_POSITION || lastVisible == RecyclerView.NO_POSITION) return

        for (i in lastVisible - 1 downTo firstVisible) {
            val holder =
                recyclerView.findViewHolderForAdapterPosition(i) as? VideoViewHolder ?: continue
            val rect = Rect()
            if (holder.itemView.getGlobalVisibleRect(rect)) {
                val video = videoMessages.getOrNull(i) ?: continue
                onGetPosition.invoke(i)
                videoPlayer.prepareAndPlay(
                    i,
                    video.assetPath,
                    holder.videoPlayerView
                )
                break
            }
        }
    }
}