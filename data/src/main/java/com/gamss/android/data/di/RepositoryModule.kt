package com.gamss.android.data.di

import com.gamss.android.data.repository.AuthRepositoryImpl
import com.gamss.android.data.repository.CardRepositoryImpl
import com.gamss.android.data.repository.ConversationRepositoryImpl
import com.gamss.android.domain.card.CardRepository
import com.gamss.android.domain.conversation.ConversationRepository
import com.gamss.android.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class RepositoryModule {

    @Binds
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    abstract fun bindConversationRepository(
        conversationRepositoryImpl: ConversationRepositoryImpl
    ): ConversationRepository

    @Binds
    abstract fun bindCardRepository(
        cardRepositoryImpl: CardRepositoryImpl
    ): CardRepository
}
