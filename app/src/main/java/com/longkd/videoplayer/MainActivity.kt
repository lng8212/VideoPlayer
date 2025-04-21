package com.longkd.videoplayer

import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.Rect
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.longkd.videoplayer.databinding.ActivityMainBinding
import com.longkd.videoplayer.model.VideoMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

@UnstableApi
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var videoAdapter: VideoMessageAdapter
    private lateinit var videoManager: VideoPlayerManager
    private var videoMessages = mutableListOf<VideoMessage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize managers first
        videoManager = VideoPlayerManager(this)
        videoAdapter = VideoMessageAdapter(videoMessages) {
            val holder =
                binding.recyclerView.findViewHolderForAdapterPosition(it) as? VideoViewHolder
                    ?: return@VideoMessageAdapter
            val video = videoMessages.getOrNull(it) ?: return@VideoMessageAdapter
            videoManager.prepareAndPlay(
                it,
                video.assetPath,
                holder.textureView,
                holder.thumbnailView
            )
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = videoAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(rv: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        autoPlayMostVisibleVideo()
                    }
                }
            })
        }

        // Add view recycler listener
        binding.recyclerView.setRecyclerListener { holder ->
            if (holder is VideoViewHolder) {
                holder.onViewRecycled()
            }
        }

        // Load video messages and pre-generate thumbnails
        loadVideoMessagesFromAssets()

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                // Auto-play when activity resumes
                binding.recyclerView.post {
                    autoPlayMostVisibleVideo()
                }
            }

            override fun onPause(owner: LifecycleOwner) {
                videoManager.pause()
            }

            override fun onDestroy(owner: LifecycleOwner) {
                videoManager.release()
            }
        })
    }

    private fun loadVideoMessagesFromAssets() {
        lifecycleScope.launch {
            try {
                val videoAssets = assets.list("videos") ?: emptyArray()

                // First add all videos to the list
                videoAssets.forEachIndexed { index, filename ->
                    if (filename.endsWith(".mp4") || filename.endsWith(".3gp")) {
                        val isSender = index % 2 == 0
                        val assetPath = "videos/$filename"

                        videoMessages.add(
                            VideoMessage(
                                id = UUID.randomUUID().toString(),
                                assetPath = assetPath,
                                isSender = isSender
                            )
                        )
                    }
                }

                // Update adapter first to show placeholder thumbnails
                videoAdapter.notifyDataSetChanged()

                // Then start pre-generating thumbnails in background
                videoMessages.forEach { video ->
                    if (ThumbnailCache.bitmapCache.get(video.assetPath) == null) {
                        withContext(Dispatchers.IO) {
                            preGenerateThumbnail(video.assetPath)
                        }
                        // Notify item updated so it can use the cached thumbnail
                        val position = videoMessages.indexOf(video)
                        if (position >= 0) {
                            withContext(Dispatchers.Main) {
                                videoAdapter.notifyItemChanged(position)
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error loading asset videos", e)
            }
        }
    }

    private suspend fun preGenerateThumbnail(assetPath: String): Bitmap? {
        var retriever: MediaMetadataRetriever? = null
        var afd: AssetFileDescriptor? = null

        try {
            retriever = MediaMetadataRetriever()
            afd = assets.openFd(assetPath)
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

    private fun autoPlayMostVisibleVideo() {
        val layoutManager = binding.recyclerView.layoutManager as? LinearLayoutManager ?: return
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        val lastVisible = layoutManager.findLastVisibleItemPosition()

        if (firstVisible == RecyclerView.NO_POSITION || lastVisible == RecyclerView.NO_POSITION) return

        for (i in lastVisible - 1 downTo firstVisible) {
            val holder =
                binding.recyclerView.findViewHolderForAdapterPosition(i) as? VideoViewHolder
                    ?: continue
            val rect = Rect()
            if (holder.itemView.getGlobalVisibleRect(rect)) {
                // This item is visible — play it
                val video = videoMessages.getOrNull(i) ?: continue
                videoManager.prepareAndPlay(
                    i,
                    video.assetPath,
                    holder.textureView,
                    holder.thumbnailView
                )
                break // Only play the first one found from bottom up
            }
        }
    }

    companion object {
        private val TAG = MainActivity::class.simpleName
    }
}