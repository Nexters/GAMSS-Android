package com.gamss.android.data.auth

import android.util.Log
import com.gamss.android.core.common.AppResult
import com.gamss.android.core.common.network.ApiException
import com.gamss.android.data.di.ApplicationScope
import com.gamss.android.data.local.auth.AuthTokenLocalDataSource
import com.gamss.android.data.local.auth.model.StoredAuthTokens
import com.gamss.android.data.remote.auth.AuthService
import com.gamss.android.data.remote.auth.model.request.LoginRequest
import com.gamss.android.data.remote.auth.model.request.RefreshTokenRequest
import com.gamss.android.data.repository.runCatchingApiCall
import com.gamss.android.domain.auth.LoginResult
import com.gamss.android.domain.auth.SessionExpiredException
import com.gamss.android.domain.auth.SessionState
import com.gamss.android.domain.auth.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AuthRepositoryImpl @Inject constructor(
    private val authService: AuthService,
    private val firebaseAuth: FirebaseAuth,
    private val authTokenLocalDataSource: AuthTokenLocalDataSource,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : AuthRepository {

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Loading)
    override val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    override suspend fun login(googleIdToken: String): AppResult<LoginResult> {
        val result = runCatchingApiCall(treatUnauthorizedAsSessionExpired = false) {
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
            LoginResult(isFirstLogin = loginResponse.isFirstLogin)
        }
        if (result is AppResult.Success) {
            _sessionState.value = SessionState.Authenticated
        }
        return result
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
        } else if (result is AppResult.Success) {
            _sessionState.value = SessionState.Authenticated
        }
        return result
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun restoreSession(): AppResult<Unit> {
        return try {
            val result = restoreStoredSession()
            if (result is AppResult.Failure) {
                _sessionState.value = SessionState.Unauthenticated
            }
            result
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            _sessionState.value = SessionState.Unauthenticated
            applicationScope.launch { invalidateSession() }
            AppResult.Failure(e)
        }
    }

    private suspend fun restoreStoredSession(): AppResult<Unit> {
        val storedTokens = authTokenLocalDataSource.getTokens()
        val accessToken = storedTokens.accessToken
        val refreshToken = storedTokens.refreshToken

        return when {
            accessToken.isNullOrBlank() && refreshToken.isNullOrBlank() -> {
                _sessionState.value = SessionState.Unauthenticated
                AppResult.Success(Unit)
            }
            accessToken.isNullOrBlank() -> reissueTokens()
            else -> {
                _sessionState.value = SessionState.Authenticated
                AppResult.Success(Unit)
            }
        }
    }

    override suspend fun logout(): AppResult<Unit> = clearSession(SessionClearReason.LoggedOut)

    private suspend fun invalidateSession(): AppResult<Unit> =
        clearSession(SessionClearReason.Expired)

    private suspend fun clearSession(reason: SessionClearReason): AppResult<Unit> {
        val result = runCatchingApiCall {
            authTokenLocalDataSource.clearTokens()
            firebaseAuth.signOut()
        }
        if (result is AppResult.Failure) {
            Log.e(TAG, "세션 정리 실패 (reason=$reason)", result.throwable)
        }
        _sessionState.value = SessionState.Unauthenticated
        return result
    }

    private enum class SessionClearReason {
        LoggedOut,
        Expired,
    }

    private companion object {
        const val MISSING_TOKEN_DATA_MESSAGE = "No available token data"
        private const val TAG = "AuthRepositoryImpl"
    }
}