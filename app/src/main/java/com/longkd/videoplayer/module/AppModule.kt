package com.longkd.videoplayer.module

import android.content.Context
import com.longkd.videoplayer.player.VideoPlayerManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideVideoPlayerManager(@ApplicationContext context: Context) =
        VideoPlayerManager(context)
}