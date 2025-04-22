package com.longkd.videoplayer.ui

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.longkd.videoplayer.databinding.DialogFullscreenBinding
import com.longkd.videoplayer.player.VideoPlayerManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FullscreenVideoDialogFragment : DialogFragment() {

    private lateinit var binding: DialogFullscreenBinding

    @Inject
    lateinit var playerManager: VideoPlayerManager

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Material_NoActionBar_Fullscreen)

        dialog.setOnShowListener {
            val window = dialog.window ?: return@setOnShowListener

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.let { controller ->
                    controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    controller.systemBarsBehavior =
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                @Suppress("DEPRECATION")
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
                )
            }
        }

        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DialogFullscreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.videoPlayerView.apply {
            setIsFullScreen(true)
            setOnToggleFullscreenListener {
                dismiss() // Exit fullscreen
            }
        }
        playerManager.attachToFullscreen(
            binding.videoPlayerView
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val window = dialog?.window ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.show(
                WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars()
            )
        }
        playerManager.detachFromFullscreen()
    }
}
