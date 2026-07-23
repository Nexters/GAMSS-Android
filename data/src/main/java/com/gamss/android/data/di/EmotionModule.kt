package com.gamss.android.data.di

import com.gamss.android.data.emotion.AndroidEmotionClassifier
import com.gamss.android.domain.emotion.EmotionClassifier
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class EmotionModule {

    // 구현체(AndroidEmotionClassifier)가 이미 @Singleton 이라 바인딩부에는 스코프를 붙이지 않는다.
    @Binds
    abstract fun bindEmotionClassifier(impl: AndroidEmotionClassifier): EmotionClassifier
}
