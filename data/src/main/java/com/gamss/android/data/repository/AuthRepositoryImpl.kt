package com.gamss.android.data.repository

import com.gamss.android.core.common.AppResult
import com.gamss.android.data.network.AuthEventBus
import com.gamss.android.data.local.auth.AuthTokenLocalDataSource
import com.gamss.android.data.local.auth.model.StoredAuthTokens
import com.gamss.android.data.remote.auth.AuthService
import com.gamss.android.data.remote.auth.model.request.LoginRequest
import com.gamss.android.data.remote.auth.model.request.RefreshTokenRequest
import com.gamss.android.data.remote.auth.model.response.AuthRequestException
import com.gamss.android.data.remote.auth.model.response.LoginResponse
import com.gamss.android.domain.model.AuthEvent
import com.gamss.android.domain.model.AuthException
import com.gamss.android.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AuthRepositoryImpl @Inject constructor(
    private val authService: AuthService,
    private val firebaseAuth: FirebaseAuth,
    private val authTokenLocalDataSource: AuthTokenLocalDataSource,
    private val authEventBus: AuthEventBus,
) : AuthRepository {

    override val authEvents: Flow<AuthEvent> = authEventBus.events

    override suspend fun login(googleIdToken: String): AppResult<Unit> {
        return runCatchingAuth {
            val credential = GoogleAuthProvider.getCredential(googleIdToken, null)

            val authResult = firebaseAuth
                .signInWithCredential(credential)
                .await()

            val firebaseIdToken = checkNotNull(
                authResult.user?.getIdToken(false)?.await()?.token
            ) { "Firebase ID token을 발급받지 못했습니다." }

            val response = authService.login(LoginRequest(idToken = firebaseIdToken))
            saveTokens(response)
        }
    }

    override suspend fun reissueTokens(): AppResult<Unit> {
        return runCatchingAuth {
            val refreshToken = authTokenLocalDataSource.getTokens().refreshToken
                ?: throw AuthException.SessionExpired()

            try {
                val response = authService.reissueTokens(
                    request = RefreshTokenRequest(refreshToken = refreshToken),
                )
                saveTokens(response)
            } catch (e: AuthRequestException) {
                // refreshToken 자체가 거부된 경우이므로 재로그인이 필요하다.
                throw AuthException.SessionExpired(e)
            }
        }
    }

    override suspend fun logout(): AppResult<Unit> {
        return runCatchingAuth {
            authTokenLocalDataSource.clearTokens()
            authEventBus.notify(AuthEvent.LoggedOut)
        }
    }

    private suspend fun saveTokens(response: LoginResponse) {
        val tokenData = response.requireTokenData()

        authTokenLocalDataSource.saveTokens(
            StoredAuthTokens(
                accessToken = tokenData.accessToken,
                refreshToken = tokenData.refreshToken,
            ),
        )
    }

    @Suppress("TooGenericExceptionCaught")
    private inline fun <T> runCatchingAuth(block: () -> T): AppResult<T> =
        try {
            AppResult.Success(block())
        } catch (e: AuthException) {
            AppResult.Failure(e)
        } catch (e: AuthRequestException) {
            AppResult.Failure(AuthException.InvalidCredentials(e.message, e))
        } catch (e: HttpException) {
            AppResult.Failure(
                if (e.code() == HTTP_UNAUTHORIZED || e.code() == HTTP_FORBIDDEN) {
                    AuthException.SessionExpired(e)
                } else {
                    AuthException.Network(e)
                },
            )
        } catch (e: IOException) {
            AppResult.Failure(AuthException.Network(e))
        } catch (e: Throwable) {
            AppResult.Failure(e)
        }

    private companion object {
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_FORBIDDEN = 403
    }
}
