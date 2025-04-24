package com.longkd.videoplayer.ui

import android.os.Bundle
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.longkd.videoplayer.R
import com.longkd.videoplayer.cache.ThumbnailCache
import com.longkd.videoplayer.databinding.FragmentListVideoBinding
import com.longkd.videoplayer.model.VideoMessage
import com.longkd.videoplayer.player.VideoPlayerManager
import com.longkd.videoplayer.services.ThumbnailService
import com.longkd.videoplayer.utils.VideoScrollDetector
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class ListVideoFragment : Fragment() {
    @Inject
    lateinit var videoPlayerManager: VideoPlayerManager

    private lateinit var thumbnailService: ThumbnailService
    private lateinit var videoScrollDetector: VideoScrollDetector
    private val videoMessages = mutableListOf<VideoMessage>()

    private lateinit var binding: FragmentListVideoBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentListVideoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = TransitionInflater.from(requireContext())
            .inflateTransition(android.R.transition.move).apply {
                duration = 300
                startDelay = 20
            }

        sharedElementReturnTransition = TransitionInflater.from(requireContext())
            .inflateTransition(android.R.transition.move).apply {
                duration = 300
                startDelay = 20
            }
        postponeEnterTransition()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDependencies()
        setupVideoList()
        setupScrollListener()
    }

    private fun initDependencies() {
        val thumbnailCache = ThumbnailCache()

        thumbnailService = ThumbnailService(requireContext(), thumbnailCache)
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
                if (videoMessages.isEmpty()) {
                    val videoAssets = requireContext().assets.list("videos") ?: emptyArray()
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
                }
            } catch (e: IOException) {
            }
        }
    }

    private fun setupVideoList() {
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = VideoMessageAdapter(
            videoMessages,
            thumbnailService,
            onItemClick = { position ->
                videoScrollDetector.playClickVideo(position)
                videoPlayerManager.setCurrentPlayerPosition(position)
            },
            onFullScreenClick = { position, transitionName ->
                launchFullscreenVideo(position = position, transitionName = transitionName)
            }
        )
    }

    private fun launchFullscreenVideo(position: Int, transitionName: String) {
        val viewHolder =
            binding.recyclerView.findViewHolderForAdapterPosition(position) as? VideoViewHolder

        val textureView = viewHolder?.videoPlayerView?.textureView ?: return

        val bundle = bundleOf("transition_name" to transitionName)

        requireActivity().supportFragmentManager.commit {
            setReorderingAllowed(true)
            addSharedElement(textureView, transitionName)
            hide(this@ListVideoFragment)
            add(R.id.main_container, FullscreenVideoFragment::class.java, bundle)
        }

    }

    private fun setupScrollListener() {
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    videoScrollDetector.detectAndPlayVisibleVideo {
                        videoPlayerManager.setCurrentPlayerPosition(position = it)
                    }
                }
            }
        })
    }

    private fun continueToPlay() {
        val position =
            if (videoPlayerManager.getCurrentPlayerPosition() != -1) videoPlayerManager.getCurrentPlayerPosition() else return
        val viewHolder =
            binding.recyclerView.findViewHolderForAdapterPosition(position) as? VideoViewHolder

        viewHolder?.videoPlayerView?.let {
            videoPlayerManager.setupSurfaceTexture(it, true) {
                startPostponedEnterTransition()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        continueToPlay()
//        binding.recyclerView.post {
//            videoScrollDetector.detectAndPlayVisibleVideo()
//        }
    }


    override fun onDestroy() {
        super.onDestroy()
        videoPlayerManager.releaseCompletely()
    }
}