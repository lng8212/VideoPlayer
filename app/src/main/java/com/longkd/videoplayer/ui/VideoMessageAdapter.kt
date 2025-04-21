package com.longkd.videoplayer.ui


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import com.longkd.videoplayer.R
import com.longkd.videoplayer.interfaces.ThumbnailProvider
import com.longkd.videoplayer.model.VideoMessage

@OptIn(UnstableApi::class)
class VideoMessageAdapter(
    private val videos: List<VideoMessage>,
    private val thumbnailProvider: ThumbnailProvider,
    private val onItemClick: (position: Int) -> Unit
) : RecyclerView.Adapter<VideoViewHolder>() {

    override fun getItemViewType(position: Int): Int {
        return if (videos[position].isSender) 0 else 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val layout = if (viewType == 0) R.layout.item_video_sent else R.layout.item_video_received
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return VideoViewHolder(view, thumbnailProvider, onItemClick)
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