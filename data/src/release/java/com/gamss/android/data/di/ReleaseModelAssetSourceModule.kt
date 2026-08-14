package com.gamss.android.data.di

import android.content.Context
import com.gamss.android.data.model.ModelAssetSource
import com.gamss.android.data.model.OnDemandModelAssets
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * `release` buildType(Play Console 배포) 전용 바인딩 — 실제 Play Asset Delivery 를 쓴다.
 * `debug`/`firebase` 는 각 소스셋의 동명 모듈(`LocalAssetsModelSource` 바인딩)로 대체된다.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object ReleaseModelAssetSourceModule {
    @Provides
    @Singleton
    fun provideModelAssetSource(@ApplicationContext context: Context): ModelAssetSource = OnDemandModelAssets(context)
}
