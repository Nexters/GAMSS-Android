package com.gamss.android.domain.repository

import com.gamss.android.core.common.AppResult

interface UserRepository {
    suspend fun updateNickname(nickname: String): AppResult<String>
}
