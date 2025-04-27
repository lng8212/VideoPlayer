package com.longkd.videoplayer.ui

import android.os.Bundle
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import com.longkd.videoplayer.databinding.FragmentFullscreenBinding
import com.longkd.videoplayer.player.VideoPlayerManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FullscreenVideoFragment : Fragment() {

    private lateinit var binding: FragmentFullscreenBinding

    @Inject
    lateinit var videoPlayerManager: VideoPlayerManager


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


    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFullscreenBinding.inflate(inflater, container, false)
        postponeEnterTransition()
        val transitionName = arguments?.getString("transition_name")
        binding.videoPlayerView.textureView.transitionName = transitionName
        return binding.root
    }

    @UnstableApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setVideoAspectRatio()
        videoPlayerManager.setupSurfaceTexture(binding.videoPlayerView, true) {
            startPostponedEnterTransition()
        }

        binding.videoPlayerView.setFullScreen()

        binding.videoPlayerView.setOnToggleFullscreenListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    @UnstableApi
    private fun setVideoAspectRatio() {
        val videoWidth = videoPlayerManager.player.videoFormat?.width ?: 16
        val videoHeight = videoPlayerManager.player.videoFormat?.height ?: 9

        binding.videoPlayerView.textureView.post {
            val parentWidth = (binding.videoPlayerView.textureView.parent as ViewGroup).width
            val parentHeight = (binding.videoPlayerView.textureView.parent as ViewGroup).height

            val aspectRatio = videoWidth.toFloat() / videoHeight.toFloat()

            val newWidth: Int
            val newHeight: Int

            if (parentWidth.toFloat() / parentHeight.toFloat() > aspectRatio) {
                newHeight = parentHeight
                newWidth = (newHeight * aspectRatio).toInt()
            } else {
                newWidth = parentWidth
                newHeight = (newWidth / aspectRatio).toInt()
            }

            val layoutParams = binding.videoPlayerView.textureView.layoutParams
            layoutParams.width = newWidth
            layoutParams.height = newHeight
            binding.videoPlayerView.textureView.layoutParams = layoutParams
        }
    }
}


