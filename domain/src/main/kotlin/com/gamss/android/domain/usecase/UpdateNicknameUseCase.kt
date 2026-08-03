package com.gamss.android.domain.usecase

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.repository.UserRepository
import javax.inject.Inject

class UpdateNicknameUseCase @Inject constructor(
    private val userRepository: UserRepository,
) : UseCase<String, AppResult<String>> {

    override suspend fun invoke(params: String): AppResult<String> =
        userRepository.updateNickname(nickname = params)
}
