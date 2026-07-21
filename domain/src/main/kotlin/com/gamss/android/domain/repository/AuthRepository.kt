package com.gamss.android.domain.repository

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.model.AuthResponse

interface AuthRepository {
    suspend fun login(userId: String): AppResult<AuthResponse>
}
