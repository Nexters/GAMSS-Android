package com.gamss.android.data.di

import com.gamss.android.data.local.auth.AuthTokenLocalDataSource
import com.gamss.android.data.local.auth.EncryptedAuthTokenLocalDataSource
import com.gamss.android.data.local.auth.TinkTokenCipher
import com.gamss.android.data.local.auth.TokenCipher
import com.gamss.android.data.local.auth.TokenProvider
import com.gamss.android.data.local.auth.TokenProviderImpl
import com.gamss.android.data.repository.AuthRepositoryImpl
import com.gamss.android.data.repository.UserRepositoryImpl
import com.gamss.android.domain.repository.AuthRepository
import com.gamss.android.domain.repository.UserRepository
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
    abstract fun bindAuthTokenLocalDataSource(
        encryptedAuthTokenLocalDataSource: EncryptedAuthTokenLocalDataSource,
    ): AuthTokenLocalDataSource

    @Binds
    abstract fun bindTokenCipher(
        tinkTokenCipher: TinkTokenCipher,
    ): TokenCipher

    @Binds
    abstract fun bindTokenProvider(
        tokenProviderImpl: TokenProviderImpl,
    ): TokenProvider

    @Binds
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl,
    ): UserRepository
}
