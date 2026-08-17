package com.gamss.android.data.di

import android.content.Context
import com.gamss.android.data.model.LocalAssetsModelSource
import com.gamss.android.data.model.ModelAssetSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * `debug` buildType(Android Studio Run) 전용 바인딩 — Play Store 를 거치지 않는 설치 경로라
 * AssetPackManager 가 동작하지 않는다. 모델은 APK 에 그대로 번들된 assets 에서 읽는다
 * (`data/build.gradle.kts` 의 `debug` sourceSet assets.srcDirs 참고). `internal`/`release` 는 동명의 다른
 * 모듈(`OnDemandModelAssets` 바인딩)로 대체된다.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object DebugModelAssetSourceModule {
    @Provides
    @Singleton
    fun provideModelAssetSource(@ApplicationContext context: Context): ModelAssetSource =
        LocalAssetsModelSource(context)
}
