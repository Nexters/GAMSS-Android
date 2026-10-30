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

    // 자동 이벤트(첫 실행, 세션)는 켜 둔다. IP 기반 위치는 국내 서비스라 쓸 데가 없어 끈다.
    @Provides
    @Singleton
    fun provideAnalyticsTracker(@ApplicationContext context: Context): AnalyticsTracker {
        val mixpanel = MixpanelAPI.getInstance(context, BuildConfig.MIXPANEL_TOKEN, true)
        mixpanel.setUseIpAddressForGeolocation(false)
        return MixpanelAnalyticsTracker(mixpanel)
    }
}
