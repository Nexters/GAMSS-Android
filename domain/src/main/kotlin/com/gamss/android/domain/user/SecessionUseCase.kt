package com.gamss.android.domain.user

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.repository.AuthRepository
import com.gamss.android.domain.usecase.NoParamUseCase
import javax.inject.Inject

class SecessionUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
) : NoParamUseCase<AppResult<Unit>> {
    override suspend fun invoke(): AppResult<Unit> {
        return when (val result = userRepository.secession()) {
            is AppResult.Success -> authRepository.logout()
            is AppResult.Failure -> result
        }
    }
}
