package com.gamss.android.data.repository

import com.gamss.android.data.remote.auth.AuthService
import com.gamss.android.data.remote.auth.model.request.LoginRequest
import com.gamss.android.domain.model.AuthResponse
import com.gamss.android.domain.repository.AuthRepository
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authService: AuthService,
) : AuthRepository {

    override suspend fun login(userId: String): AuthResponse =
        authService.login(LoginRequest(userId = userId)).toDomain()
}