package com.gamss.android.domain.usecase

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.repository.AuthRepository
import com.gamss.android.domain.repository.UserRepository
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
