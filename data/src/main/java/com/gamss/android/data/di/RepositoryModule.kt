package com.gamss.android.data.di

import com.gamss.android.data.local.auth.AuthTokenLocalDataSource
import com.gamss.android.data.local.auth.EncryptedAuthTokenLocalDataSource
import com.gamss.android.data.local.auth.TinkTokenCipher
import com.gamss.android.data.local.auth.TokenCipher
import com.gamss.android.data.local.auth.TokenProvider
import com.gamss.android.data.local.auth.TokenProviderImpl
import com.gamss.android.data.repository.AuthRepositoryImpl
import com.gamss.android.data.repository.ConversationRepositoryImpl
import com.gamss.android.data.repository.TokenUsageRefreshNotifierImpl
import com.gamss.android.data.repository.UserRepositoryImpl
import com.gamss.android.domain.conversation.ConversationRepository
import com.gamss.android.domain.repository.AuthRepository
import com.gamss.android.domain.repository.TokenUsageRefreshNotifier
import com.gamss.android.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
internal abstract class RepositoryModule {

    @Binds
    abstract fun bindAuthRepository(authRepositoryImpl: AuthRepositoryImpl): AuthRepository

    @Binds
    abstract fun bindConversationRepository(
        conversationRepositoryImpl: ConversationRepositoryImpl,
    ): ConversationRepository

    @Binds
    abstract fun bindTokenUsageRefreshNotifier(
        tokenUsageRefreshNotifierImpl: TokenUsageRefreshNotifierImpl,
    ): TokenUsageRefreshNotifier

    @Binds
    abstract fun bindAuthTokenLocalDataSource(
        encryptedAuthTokenLocalDataSource: EncryptedAuthTokenLocalDataSource,
    ): AuthTokenLocalDataSource

    @Binds
    abstract fun bindTokenCipher(tinkTokenCipher: TinkTokenCipher): TokenCipher

    @Binds
    abstract fun bindTokenProvider(tokenProviderImpl: TokenProviderImpl): TokenProvider

    @Binds
    abstract fun bindUserRepository(userRepositoryImpl: UserRepositoryImpl): UserRepository

    companion object {
        @Provides
        @Singleton
        @ApplicationScope
        fun provideApplicationScope(): CoroutineScope =
            CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
