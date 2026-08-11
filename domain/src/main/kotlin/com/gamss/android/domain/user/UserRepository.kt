package com.gamss.android.domain.user

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.model.DailyTokenUsage

interface UserRepository {
    suspend fun updateNickname(nickname: String): AppResult<UserProfile>

    suspend fun deleteUserAccount(): AppResult<Unit>

    suspend fun getUserInfo(): AppResult<UserProfile>

    suspend fun getDailyTokenUsage(): AppResult<DailyTokenUsage>
}
