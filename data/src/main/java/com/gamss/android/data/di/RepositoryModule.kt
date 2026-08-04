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
import com.gamss.android.domain.user.UserRepository
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

/**
 * 특정 화면의 생명주기와 무관하게 앱 프로세스 동안 유지되어야 하는 작업(세션 무효화 등)에 사용한다.
 * SupervisorJob이라 한 작업의 실패가 다른 작업을 취소시키지 않는다.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class ApplicationScope

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

    companion object {
        @Provides
        @Singleton
        @ApplicationScope
        fun provideApplicationScope(): CoroutineScope =
            CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
