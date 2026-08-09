package com.gamss.android.data.repository

import com.gamss.android.core.common.AppResult
import com.gamss.android.core.common.mapFailure
import com.gamss.android.core.common.network.ApiException
import com.gamss.android.data.remote.user.UserService
import com.gamss.android.data.remote.user.model.request.UpdateNicknameRequest
import com.gamss.android.data.remote.user.model.response.toDomain
import com.gamss.android.domain.user.NicknameUpdateException
import com.gamss.android.domain.user.UserProfile
import com.gamss.android.domain.user.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class UserRepositoryImpl @Inject constructor(
    private val userService: UserService,
) : UserRepository {

    override suspend fun updateNickname(nickname: String): AppResult<UserProfile> {
        return runCatchingApiCall {
            val response = userService.updateNickname(UpdateNicknameRequest(nickname = nickname))
            checkNotNull(response.data) { "No available nickname data" }.toDomain()
        }.mapFailure {
            it.toNicknameUpdateException()
        }
    }

    override suspend fun deleteUserAccount(): AppResult<Unit> {
        return runCatchingApiCall {
            userService.deleteUserAccount()
        }
    }

    override suspend fun getUserInfo(): AppResult<UserProfile> {
        return runCatchingApiCall {
            checkNotNull(userService.getUserInfo().data) { "No available user info data" }.toDomain()
        }
    }
}

private fun Throwable.toNicknameUpdateException(): Throwable =
    if (this is ApiException.Http) {
        when (code) {
            ERROR_INVALID_INPUT -> NicknameUpdateException.MissingNickname(cause = this)
            ERROR_INVALID_NICKNAME -> NicknameUpdateException.InvalidNickname(cause = this)
            else -> this
        }
    } else {
        this
    }

private const val ERROR_INVALID_INPUT = "INVALID_INPUT"
private const val ERROR_INVALID_NICKNAME = "INVALID_NICKNAME"
