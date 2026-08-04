package com.gamss.android.domain.user

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.user.UserRepository
import com.gamss.android.domain.usecase.UseCase
import javax.inject.Inject

class UpdateNicknameUseCase @Inject constructor(
    private val userRepository: UserRepository,
) : UseCase<String, AppResult<UserProfile>> {

    override suspend fun invoke(params: String): AppResult<UserProfile> =
        userRepository.updateNickname(nickname = params)
}