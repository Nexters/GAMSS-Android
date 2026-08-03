package com.gamss.android.data.di

import com.gamss.android.data.remote.card.CardService
import com.gamss.android.data.remote.conversation.ConversationService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/** 대화 흐름(메시지·종료·카드) 서비스. NetworkModule 은 클라이언트·Retrofit 구성만 갖는다. */
@Module
@InstallIn(SingletonComponent::class)
internal object ConversationNetworkModule {

    @Provides
    @Singleton
    fun provideConversationService(retrofit: Retrofit): ConversationService =
        retrofit.create(ConversationService::class.java)

    @Provides
    @Singleton
    fun provideCardService(retrofit: Retrofit): CardService =
        retrofit.create(CardService::class.java)
}
