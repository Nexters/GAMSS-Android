package com.gamss.android.domain.usecase

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.model.AuthResponse
import com.gamss.android.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) : UseCase<String, AppResult<AuthResponse>> {

    override suspend fun invoke(params: String): AppResult<AuthResponse> =
        authRepository.login(googleIdToken = params)
}
