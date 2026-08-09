package com.gamss.android.domain.user

import com.gamss.android.core.common.AppResult

interface UserRepository {
    suspend fun updateNickname(nickname: String): AppResult<UserProfile>

    suspend fun deleteUserAccount(): AppResult<Unit>

    suspend fun getUserInfo(): AppResult<UserProfile>
}
