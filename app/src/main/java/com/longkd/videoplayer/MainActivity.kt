package com.longkd.videoplayer

import android.graphics.SurfaceTexture
import android.os.Bundle
import android.view.Surface
import android.view.TextureView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.longkd.videoplayer.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), TextureView.SurfaceTextureListener {
    private lateinit var binding: ActivityMainBinding
    private var surface: Surface? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NativePlayer.init()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.textureView.surfaceTextureListener = this

        binding.btnPlay.setOnClickListener {
            surface?.let {
                lifecycleScope.launch(Dispatchers.Default) {
                    val videoPath = copyAssetToInternalStorage(this@MainActivity, "sample.mp4")

                    NativePlayer.playVideo(videoPath, it)
                }
            }
        }

        binding.btnPause.setOnClickListener {
            NativePlayer.pauseVideo()
        }

        binding.btnResume.setOnClickListener {
            NativePlayer.resumeVideo()
        }

        binding.btnStop.setOnClickListener {
            NativePlayer.stopVideo()
        }
    }

    override fun onSurfaceTextureAvailable(
        surfaceTexture: SurfaceTexture,
        width: Int,
        height: Int
    ) {
        surface = Surface(surfaceTexture)
    }

    override fun onSurfaceTextureSizeChanged(
        surface: SurfaceTexture,
        width: Int,
        height: Int
    ) {
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        surface.release()
        NativePlayer.stopVideo()
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
    }
}

