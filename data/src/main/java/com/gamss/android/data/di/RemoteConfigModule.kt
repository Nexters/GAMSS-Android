package com.gamss.android.data.di

import com.gamss.android.data.config.RemoteConfigRepositoryImpl
import com.gamss.android.domain.config.RemoteConfigRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class RemoteConfigModule {

    @Binds
    abstract fun bindRemoteConfigRepository(
        remoteConfigRepositoryImpl: RemoteConfigRepositoryImpl,
    ): RemoteConfigRepository
}
