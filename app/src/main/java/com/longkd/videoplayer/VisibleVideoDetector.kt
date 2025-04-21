package com.longkd.videoplayer

import android.graphics.Rect
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.longkd.videoplayer.model.VideoMessage

@OptIn(UnstableApi::class)
class VisibleVideoDetector(
    private val recyclerView: RecyclerView,
    private val videoMessages: List<VideoMessage>,
    private val videoPlayerManager: VideoPlayerManager
) {

    fun playFirstFullyVisible() {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        val lastVisible = layoutManager.findLastVisibleItemPosition()
        if (firstVisible == RecyclerView.NO_POSITION || lastVisible == RecyclerView.NO_POSITION) return

        for (i in lastVisible downTo firstVisible) {
            val holder =
                recyclerView.findViewHolderForAdapterPosition(i) as? VideoViewHolder ?: continue
            val rect = Rect()
            if (holder.itemView.getGlobalVisibleRect(rect)) {
                val video = videoMessages.getOrNull(i) ?: continue
                videoPlayerManager.prepareAndPlay(
                    i,
                    video.assetPath,
                    holder.textureView,
                    holder.thumbnailView
                )
                break
            }
        }
    }
}
