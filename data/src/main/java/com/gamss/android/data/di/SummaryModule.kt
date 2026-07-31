package com.gamss.android.data.di

import com.gamss.android.data.summary.AndroidDiarySummarizer
import com.gamss.android.domain.summary.DiarySummarizer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SummaryModule {

    // 구현체(AndroidDiarySummarizer)가 이미 @Singleton 이라 바인딩부에는 스코프를 붙이지 않는다.
    @Binds
    abstract fun bindDiarySummarizer(impl: AndroidDiarySummarizer): DiarySummarizer
}
