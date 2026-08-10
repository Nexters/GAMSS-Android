package com.gamss.android.domain.user

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.auth.AuthRepository
import com.gamss.android.domain.usecase.NoParamUseCase
import javax.inject.Inject

class DeleteUserAccountUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
) : NoParamUseCase<AppResult<Unit>> {
    override suspend fun invoke(): AppResult<Unit> {
        val deleteResult = userRepository.deleteUserAccount()
        if (deleteResult is AppResult.Failure) return deleteResult
        authRepository.logout()
        return AppResult.Success(Unit)
    }
}
