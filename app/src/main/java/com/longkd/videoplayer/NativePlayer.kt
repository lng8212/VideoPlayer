package com.longkd.videoplayer

import android.view.Surface

object NativePlayer {
    init {
        System.loadLibrary("native-lib")
    }

    external fun init()
    external fun playVideo(path: String, surface: Surface)
    external fun pauseVideo()
    external fun resumeVideo()
    external fun stopVideo()
}
