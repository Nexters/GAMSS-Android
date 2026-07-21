package com.gamss.android.data.di

import com.gamss.android.data.emotion.AndroidEmotionClassifier
import com.gamss.android.domain.emotion.EmotionClassifier
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class EmotionModule {

    @Binds
    @Singleton
    abstract fun bindEmotionClassifier(impl: AndroidEmotionClassifier): EmotionClassifier
}
