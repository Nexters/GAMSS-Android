package com.gamss.android.domain.user

import com.gamss.android.core.common.AppResult
import com.gamss.android.core.common.network.ApiException
import com.gamss.android.domain.usecase.UseCase
import javax.inject.Inject

class UpdateNicknameUseCase @Inject constructor(
    private val userRepository: UserRepository,
) : UseCase<String, AppResult<UserProfile>> {

    override suspend fun invoke(params: String): AppResult<UserProfile> {
        val nickname = params.trim()

        validate(nickname)?.let { exception ->
            return AppResult.Failure(exception)
        }

        return when (val result = userRepository.updateNickname(nickname = nickname)) {
            is AppResult.Success -> result
            is AppResult.Failure -> AppResult.Failure(result.throwable.toNicknameUpdateException())
        }
    }

    private fun validate(nickname: String): NicknameUpdateException? {
        return when {
            nickname.isBlank() -> NicknameUpdateException.MissingNickname()
            nickname.length < MIN_NICKNAME_LENGTH -> NicknameUpdateException.InvalidNickname()
            nickname.length > MAX_NICKNAME_LENGTH -> NicknameUpdateException.InvalidNickname()
            else -> null
        }
    }

    private fun Throwable.toNicknameUpdateException(): Throwable =
        if (this is ApiException.Http) {
            when (code) {
                ERROR_INVALID_INPUT -> NicknameUpdateException.MissingNickname()
                ERROR_INVALID_NICKNAME -> NicknameUpdateException.InvalidNickname()
                else -> this
            }
        } else {
            this
        }

    sealed class NicknameUpdateException : RuntimeException() {
        class MissingNickname : NicknameUpdateException()
        class InvalidNickname : NicknameUpdateException()
    }

    companion object {
        const val MIN_NICKNAME_LENGTH = 2
        const val MAX_NICKNAME_LENGTH = 20

        private const val ERROR_INVALID_INPUT = "INVALID_INPUT"
        private const val ERROR_INVALID_NICKNAME = "INVALID_NICKNAME"
    }
}
