package com.gamss.android.data.repository

import com.gamss.android.core.common.AppResult
import com.gamss.android.data.local.auth.AuthTokenLocalDataSource
import com.gamss.android.data.local.auth.model.StoredAuthTokens
import com.gamss.android.data.network.AuthEventBus
import com.gamss.android.data.remote.auth.AuthService
import com.gamss.android.data.remote.auth.model.request.LoginRequest
import com.gamss.android.data.remote.auth.model.request.RefreshTokenRequest
import com.gamss.android.data.remote.auth.model.response.LoginResponse
import com.gamss.android.data.remote.user.UserService
import com.gamss.android.domain.model.AuthEvent
import com.gamss.android.domain.model.SessionExpiredException
import com.gamss.android.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AuthRepositoryImpl @Inject constructor(
    private val authService: AuthService,
    private val userService: UserService,
    private val firebaseAuth: FirebaseAuth,
    private val authTokenLocalDataSource: AuthTokenLocalDataSource,
    private val authEventBus: AuthEventBus,
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
            saveTokens(response)
        }
    }

    override suspend fun reissueTokens(): AppResult<Unit> {
        return runCatchingApiCall {
            val refreshToken = authTokenLocalDataSource.getTokens().refreshToken
                ?: throw SessionExpiredException()

            // refreshToken이 거부되면 서버가 401/403으로 응답하므로,
            // runCatchingApiCall의 기본 처리(401/403 -> SessionExpiredException)를 따른다.
            val response = authService.reissueTokens(
                request = RefreshTokenRequest(refreshToken = refreshToken),
            )
            saveTokens(response)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun restoreSession(): AppResult<Unit> {
        val storedAccessToken = try {
            authTokenLocalDataSource.getTokens().accessToken
        } catch (e: Throwable) {
            return AppResult.Failure(e)
        }

        if (storedAccessToken != null) return AppResult.Success(Unit)

        val reissueResult = reissueTokens()
        if (reissueResult is AppResult.Failure) {
            // refreshToken이 만료/거부되어 세션을 복구할 수 없으므로 남아있는 토큰도 정리한다.
            authTokenLocalDataSource.clearTokens()
        }
        return reissueResult
    }

    override suspend fun logout(): AppResult<Unit> {
        return runCatchingApiCall {
            authTokenLocalDataSource.clearTokens()
            authEventBus.notify(AuthEvent.LoggedOut)
        }
    }

    override suspend fun secession(): AppResult<Unit> {
        // 회원 탈퇴는 accessToken이 필요한 인증된 API이므로, TokenInterceptor/TokenAuthenticator가
        // 붙어있는 일반 클라이언트(UserService)로 호출한다.
        val deleteResult = runCatchingApiCall {
            userService.secessionUser()
        }
        return when (deleteResult) {
            is AppResult.Success -> logout()
            is AppResult.Failure -> deleteResult
        }
    }

    private suspend fun saveTokens(response: LoginResponse) {
        val tokenData = checkNotNull(response.data) {
            "No available token data"
        }

        authTokenLocalDataSource.saveTokens(
            StoredAuthTokens(
                accessToken = tokenData.accessToken,
                refreshToken = tokenData.refreshToken,
            ),
        )
    }
}
