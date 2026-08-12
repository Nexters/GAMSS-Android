package com.gamss.android.data.di

import com.gamss.android.data.summary.AndroidDiarySummarizer
import com.gamss.android.data.summary.KobartTokenCounter
import com.gamss.android.domain.summary.DiarySummarizer
import com.gamss.android.domain.summary.UtteranceTokenCounter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** kobart 모델은 :models:summary-pack(on-demand 애셋팩)에서 내려받는다. 다운로드 전 요약을 요청하면
 * [com.gamss.android.data.model.ModelPackUnavailableException] 로 실패한다. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class SummaryModule {

    // 구현체(AndroidDiarySummarizer)가 이미 @Singleton 이라 바인딩부에는 스코프를 붙이지 않는다.
    @Binds
    abstract fun bindDiarySummarizer(impl: AndroidDiarySummarizer): DiarySummarizer

    @Binds
    abstract fun bindUtteranceTokenCounter(impl: KobartTokenCounter): UtteranceTokenCounter
}
