package com.gamss.android.app.di

import com.gamss.android.app.BuildConfig
import com.gamss.android.core.common.BuildInfo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** BuildConfig.DEBUG 는 모듈마다 따로 생성되므로 app 의 값 하나만 주입해 쓴다. */
@Module
@InstallIn(SingletonComponent::class)
object BuildInfoModule {

    @Provides
    @Singleton
    fun provideBuildInfo(): BuildInfo = AppBuildInfo
}

private object AppBuildInfo : BuildInfo {
    override val isDebug: Boolean = BuildConfig.DEBUG
}
