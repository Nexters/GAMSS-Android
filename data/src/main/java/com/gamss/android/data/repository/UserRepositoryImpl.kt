package com.gamss.android.data.repository

import com.gamss.android.core.common.AppResult
import com.gamss.android.data.remote.user.UserService
import com.gamss.android.data.remote.user.model.request.UpdateNicknameRequest
import com.gamss.android.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class UserRepositoryImpl @Inject constructor(
    private val userService: UserService,
) : UserRepository {

    override suspend fun updateNickname(nickname: String): AppResult<String> {
        return runCatchingApiCall {
            val response = userService.updateNickname(UpdateNicknameRequest(nickname = nickname))
            checkNotNull(response.data) {
                "No available nickname data"
            }
        }
    }

    override suspend fun secession(): AppResult<Unit> {
        return runCatchingApiCall {
            userService.secessionUser()
        }
    }
}
