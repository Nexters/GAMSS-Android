package com.gamss.android.domain.repository

import com.gamss.android.domain.model.AuthResponse

interface AuthRepository {
    suspend fun login(userId: String): AuthResponse
}
