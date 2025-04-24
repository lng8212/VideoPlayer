package com.longkd.videoplayer.ui

import android.os.Bundle
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.media3.common.util.UnstableApi
import com.longkd.videoplayer.R
import com.longkd.videoplayer.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@OptIn(UnstableApi::class)
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            add(R.id.main_container, ListVideoFragment::class.java, null)
            addToBackStack(null)
        }
    }
}