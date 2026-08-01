package com.gamss.android.data.repository

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
    override suspend fun restoreSession(): AppResult<Unit> {
        return try {
            restoreStoredSession()
        } catch (e: Throwable) {
            AppResult.Failure(e)
        }
    }

    private suspend fun restoreStoredSession(): AppResult<Unit> {
        val storedAccessToken = authTokenLocalDataSource.getTokens().accessToken
        return if (storedAccessToken.isNullOrBlank()) {
            reissueTokens()
        } else {
            AppResult.Success(Unit)
        }
    }

    override suspend fun logout(): AppResult<Unit> = clearSession(AuthEvent.LoggedOut)

    private suspend fun invalidateSession(): AppResult<Unit> =
        clearSession(AuthEvent.SessionExpired)

    private suspend fun clearSession(event: AuthEvent): AppResult<Unit> {
        return runCatchingApiCall {
            authTokenLocalDataSource.clearTokens()
            firebaseAuth.signOut()
            authEventBus.notify(event)
        }
    }

    private companion object {
        const val MISSING_TOKEN_DATA_MESSAGE = "No available token data"
    }
}
