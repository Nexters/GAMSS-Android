package com.gamss.android.domain.repository

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.model.DailyTokenUsage

interface UserRepository {
    suspend fun updateNickname(nickname: String): AppResult<String>

    suspend fun secession(): AppResult<Unit>

    suspend fun getDailyTokenUsage(): AppResult<DailyTokenUsage>
}
