package com.gamss.android.domain.user

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.usecase.NoParamUseCase
import javax.inject.Inject

class GetUserInfoUseCase @Inject constructor(
    private val userRepository: UserRepository,
): NoParamUseCase<AppResult<UserProfile>> {
    override suspend fun invoke(): AppResult<UserProfile> = userRepository.getUserInfo()
}