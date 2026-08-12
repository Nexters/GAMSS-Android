package com.gamss.android.domain.auth

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.usecase.UseCase
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) : UseCase<String, AppResult<LoginResult>> {

    override suspend fun invoke(params: String): AppResult<LoginResult> =
        authRepository.login(googleIdToken = params)
}
