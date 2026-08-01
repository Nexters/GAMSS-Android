package com.gamss.android.data.repository

import android.util.Log
import com.gamss.android.core.common.AppResult
import com.gamss.android.core.common.map
import com.gamss.android.core.common.network.ApiException
import com.gamss.android.data.auth.AuthEventBus
import com.gamss.android.data.di.ApplicationScope
import com.gamss.android.data.local.auth.AuthTokenLocalDataSource
import com.gamss.android.data.local.auth.model.StoredAuthTokens
import com.gamss.android.data.remote.auth.AuthService
import com.gamss.android.data.remote.auth.model.request.LoginRequest
import com.gamss.android.data.remote.auth.model.request.RefreshTokenRequest
import com.gamss.android.domain.model.AuthEvent
import com.gamss.android.domain.model.SessionExpiredException
import com.gamss.android.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AuthRepositoryImpl @Inject constructor(
    private val authService: AuthService,
    private val firebaseAuth: FirebaseAuth,
    private val authTokenLocalDataSource: AuthTokenLocalDataSource,
    private val authEventBus: AuthEventBus,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : AuthRepository {

    override val authEvents: Flow<AuthEvent> = authEventBus.events

    override suspend fun login(googleIdToken: String): AppResult<Unit> {
        return runCatchingApiCall(treatUnauthorizedAsSessionExpired = false) {
            val credential = GoogleAuthProvider.getCredential(googleIdToken, null)

            val authResult = firebaseAuth
                .signInWithCredential(credential)
                .await()

            val firebaseIdToken = checkNotNull(
                authResult.user?.getIdToken(false)?.await()?.token
            ) { "Firebase ID token을 발급받지 못했습니다." }

            val response = authService.login(LoginRequest(idToken = firebaseIdToken))
            val loginResponse = checkNotNull(response.data) { MISSING_TOKEN_DATA_MESSAGE }
            check(loginResponse.accessToken.isNotBlank() && loginResponse.refreshToken.isNotBlank()) {
                "Issued tokens must not be blank"
            }
            authTokenLocalDataSource.saveTokens(
                StoredAuthTokens(
                    accessToken = loginResponse.accessToken,
                    refreshToken = loginResponse.refreshToken,
                ),
            )
        }
    }

    override suspend fun reissueTokens(): AppResult<Unit> {
        val result = runCatchingApiCall {
            val refreshToken = authTokenLocalDataSource.getTokens().refreshToken
                ?.takeIf(String::isNotBlank)
                ?: throw SessionExpiredException()

            val response = authService.reissueTokens(
                request = RefreshTokenRequest(refreshToken = refreshToken),
            )
            val loginResponse = checkNotNull(response.data) { MISSING_TOKEN_DATA_MESSAGE }
            check(loginResponse.accessToken.isNotBlank() && loginResponse.refreshToken.isNotBlank()) {
                "Issued tokens must not be blank"
            }
            authTokenLocalDataSource.saveTokens(
                StoredAuthTokens(
                    accessToken = loginResponse.accessToken,
                    refreshToken = loginResponse.refreshToken,
                ),
            )
        }
        if (result is AppResult.Failure && result.throwable !is ApiException.Network) {
            applicationScope.launch { invalidateSession() }
        }
        return result
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun restoreSession(): AppResult<Boolean> {
        return try {
            restoreStoredSession()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            applicationScope.launch { invalidateSession() }
            AppResult.Failure(e)
        }
    }

    private suspend fun restoreStoredSession(): AppResult<Boolean> {
        val storedTokens = authTokenLocalDataSource.getTokens()
        val accessToken = storedTokens.accessToken
        val refreshToken = storedTokens.refreshToken

        return when {
            accessToken.isNullOrBlank() && refreshToken.isNullOrBlank() -> {
                AppResult.Success(false)
            }
            accessToken.isNullOrBlank() -> {
                reissueTokens().map { true }
            }
            else -> AppResult.Success(true)
        }
    }

    override suspend fun logout(): AppResult<Unit> = clearSession(AuthEvent.LoggedOut)

    private suspend fun invalidateSession(): AppResult<Unit> =
        clearSession(AuthEvent.SessionExpired)

    private suspend fun clearSession(event: AuthEvent): AppResult<Unit> {
        val result = runCatchingApiCall {
            authTokenLocalDataSource.clearTokens()
            firebaseAuth.signOut()
        }
        if (result is AppResult.Failure) {
            Log.e(TAG, "세션 정리 실패 (event=$event)", result.throwable)
        }
        authEventBus.notify(event)
        return result
    }

    private companion object {
        const val MISSING_TOKEN_DATA_MESSAGE = "No available token data"
        private const val TAG = "AuthRepositoryImpl"
    }
}
