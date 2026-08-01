package com.gamss.android.domain.usecase

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.repository.AuthRepository
import javax.inject.Inject

class RestoreSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) : NoParamUseCase<AppResult<Boolean>> {

    override suspend fun invoke(): AppResult<Boolean> = authRepository.restoreSession()
}
