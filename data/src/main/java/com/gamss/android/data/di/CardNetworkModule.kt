package com.gamss.android.data.di

import com.gamss.android.data.remote.card.CardService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object CardNetworkModule {

    @Provides
    @Singleton
    fun provideCardService(retrofit: Retrofit): CardService =
        retrofit.create(CardService::class.java)
}
