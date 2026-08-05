package com.gamss.android.data.di

import com.gamss.android.data.summary.AndroidDiarySummarizer
import com.gamss.android.data.summary.KobartTokenCounter
import com.gamss.android.domain.summary.DiarySummarizer
import com.gamss.android.domain.summary.UtteranceTokenCounter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * kobart 모델 asset 은 `data/src/debug/assets` 에만 있다. release 에서 요약을 요청하면 asset 이 없어 실패한다.
 * 요약을 release 경로에 붙이려면 asset 을 main 으로 되돌려야 한다.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class SummaryModule {

    // 구현체(AndroidDiarySummarizer)가 이미 @Singleton 이라 바인딩부에는 스코프를 붙이지 않는다.
    @Binds
    abstract fun bindDiarySummarizer(impl: AndroidDiarySummarizer): DiarySummarizer

    @Binds
    abstract fun bindUtteranceTokenCounter(impl: KobartTokenCounter): UtteranceTokenCounter
}
