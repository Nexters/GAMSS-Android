package com.gamss.android.domain.usecase

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) : UseCase<String, AppResult<Unit>> {

    override suspend fun invoke(params: String): AppResult<Unit> =
        authRepository.login(googleIdToken = params)
}
