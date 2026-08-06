package com.gamss.android.domain.user

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.usecase.UseCase
import javax.inject.Inject

class UpdateNicknameUseCase @Inject constructor(
    private val userRepository: UserRepository,
) : UseCase<String, AppResult<UserProfile>> {

    override suspend fun invoke(params: String): AppResult<UserProfile> {
        val nickname = params.trim()

        validate(nickname)?.let {
            return AppResult.Failure(it)
        }

        return userRepository.updateNickname(nickname)
    }

    private fun validate(nickname: String): NicknameUpdateException? {
        return when {
            nickname.isBlank() -> NicknameUpdateException.MissingNickname()
            nickname.length < MIN_NICKNAME_LENGTH -> NicknameUpdateException.InvalidLength()
            nickname.length > MAX_NICKNAME_LENGTH -> NicknameUpdateException.InvalidLength()
            else -> null
        }
    }

    companion object {
        const val MIN_NICKNAME_LENGTH = 2
        const val MAX_NICKNAME_LENGTH = 20
    }
}
