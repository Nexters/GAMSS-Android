package com.gamss.android.data.di

import com.gamss.android.data.remote.chattingRoomSearch.ChattingRoomSearchService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object SearchConversationModule {
    @Provides
    @Singleton
    fun provideChattingRoomSearchService(retrofit: Retrofit): ChattingRoomSearchService =
        retrofit.create(ChattingRoomSearchService::class.java)
}
