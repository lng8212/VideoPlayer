package com.longkd.videoplayer.ui

import android.os.Bundle
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.longkd.videoplayer.cache.ThumbnailCache
import com.longkd.videoplayer.databinding.ActivityMainBinding
import com.longkd.videoplayer.model.VideoMessage
import com.longkd.videoplayer.player.VideoPlayerManager
import com.longkd.videoplayer.services.ThumbnailService
import com.longkd.videoplayer.utils.VideoScrollDetector
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

@OptIn(UnstableApi::class)
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    @Inject
    lateinit var videoPlayerManager: VideoPlayerManager

    private lateinit var thumbnailService: ThumbnailService
    private lateinit var videoScrollDetector: VideoScrollDetector
    private val videoMessages = mutableListOf<VideoMessage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initDependencies()
        setupVideoList()
        setupScrollListener()
    }

    private fun initDependencies() {
        // Initialize dependencies
        val thumbnailCache = ThumbnailCache()
        thumbnailService = ThumbnailService(this, thumbnailCache)

        // Load video messages (replace with your actual data source)
        loadVideoMessagesFromAssets()

        videoScrollDetector = VideoScrollDetector(
            binding.recyclerView,
            videoPlayerManager,
            videoMessages
        )
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

            } catch (e: IOException) {
            }
        }
    }

    private fun setupVideoList() {
        // Set up RecyclerView
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = VideoMessageAdapter(
            videoMessages,
            thumbnailService
        ) { position ->
            // Handle item click
            videoScrollDetector.playClickVideo(position)
        }
    }

    private fun setupScrollListener() {
        // Set up scroll listener to handle video playback
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    videoScrollDetector.detectAndPlayVisibleVideo()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        binding.recyclerView.post {
            videoScrollDetector.detectAndPlayVisibleVideo()
        }
    }

    override fun onPause() {
        super.onPause()
        videoPlayerManager.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        videoPlayerManager.releaseCompletely()
    }
}