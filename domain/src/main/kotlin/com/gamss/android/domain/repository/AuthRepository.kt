package com.gamss.android.domain.repository

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.model.AuthResponse

interface AuthRepository {
    suspend fun login(googleIdToken: String): AppResult<AuthResponse>

    suspend fun getStoredTokens(): AppResult<AuthResponse>

    suspend fun clearStoredTokens(): AppResult<Unit>
}
