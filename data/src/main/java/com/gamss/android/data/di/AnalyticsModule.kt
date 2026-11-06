package com.gamss.android.data.di

import android.content.Context
import com.gamss.android.data.BuildConfig
import com.gamss.android.data.analytics.MixpanelAnalyticsTracker
import com.gamss.android.domain.analytics.AnalyticsTracker
import com.mixpanel.android.mpmetrics.MixpanelAPI
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AnalyticsModule {

    // 첫 실행, 세션 자동 이벤트는 활성화한다. IP 주소로 위치를 추정하는 기능은 국가나 도시별로 나눠 볼
    // 계획이 없어 비활성화한다.
    @Provides
    @Singleton
    fun provideAnalyticsTracker(@ApplicationContext context: Context): AnalyticsTracker {
        val mixpanel = MixpanelAPI.getInstance(context, BuildConfig.MIXPANEL_TOKEN, true)
        mixpanel.setUseIpAddressForGeolocation(false)
        return MixpanelAnalyticsTracker(mixpanel)
    }
}
