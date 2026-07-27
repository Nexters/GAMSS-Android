package com.gamss.android.domain.repository

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.model.AuthEvent
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val authEvents: Flow<AuthEvent>

    suspend fun login(googleIdToken: String): AppResult<Unit>

    suspend fun reissueTokens(): AppResult<Unit>

    suspend fun restoreSession(): AppResult<Unit>

    suspend fun logout(): AppResult<Unit>
}
