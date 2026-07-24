package com.gamss.android.data.repository

import com.gamss.android.core.common.AppResult
import com.gamss.android.data.local.auth.AuthTokenLocalDataSource
import com.gamss.android.data.local.auth.model.StoredAuthTokens
import com.gamss.android.data.remote.auth.AuthService
import com.gamss.android.data.remote.auth.model.request.LoginRequest
import com.gamss.android.domain.model.AuthResponse
import com.gamss.android.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

internal class AuthRepositoryImpl @Inject constructor(
    private val authService: AuthService,
    private val firebaseAuth: FirebaseAuth,
    private val authTokenLocalDataSource: AuthTokenLocalDataSource,
) : AuthRepository {

    override suspend fun login(googleIdToken: String): AppResult<AuthResponse> {
        return AppResult.of {
            val credential = GoogleAuthProvider.getCredential(googleIdToken, null)

            val authResult = firebaseAuth
                .signInWithCredential(credential)
                .await()

            val firebaseIdToken = checkNotNull(
                authResult.user?.getIdToken(false)?.await()?.token
            ) { "Firebase ID token을 발급받지 못했습니다." }

            val response = authService.login(LoginRequest(idToken = firebaseIdToken))
            val authResponse = response.toDomain()
            authTokenLocalDataSource.saveTokens(authResponse.toStoredTokens())
            authResponse
        }
    }

    override suspend fun getStoredTokens(): AppResult<AuthResponse> =
        AppResult.of { authTokenLocalDataSource.getTokens().toDomain() }

    override suspend fun clearStoredTokens(): AppResult<Unit> =
        AppResult.of { authTokenLocalDataSource.clearTokens() }

    private fun AuthResponse.toStoredTokens(): StoredAuthTokens =
        StoredAuthTokens(
            accessToken = accessToken,
            refreshToken = refreshToken,
        )

    private fun StoredAuthTokens.toDomain(): AuthResponse =
        AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
        )
}
