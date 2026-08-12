package com.gamss.android.domain.auth

import com.gamss.android.core.common.AppResult
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val sessionState: StateFlow<SessionState>

    suspend fun login(googleIdToken: String): AppResult<LoginResult>

    suspend fun reissueTokens(): AppResult<Unit>

    suspend fun restoreSession(): AppResult<Unit>

    suspend fun logout(): AppResult<Unit>
}
