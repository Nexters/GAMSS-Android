package com.gamss.android.domain.usecase

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.repository.AuthRepository
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) : NoParamUseCase<AppResult<Unit>> {

    override suspend fun invoke(): AppResult<Unit> = authRepository.logout()
}
