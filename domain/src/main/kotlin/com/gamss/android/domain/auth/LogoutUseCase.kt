package com.gamss.android.domain.auth

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.push.UnregisterCurrentDeviceTokenUseCase
import com.gamss.android.domain.usecase.NoParamUseCase
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val unregisterCurrentDeviceToken: UnregisterCurrentDeviceTokenUseCase,
) : NoParamUseCase<AppResult<Unit>> {

    override suspend fun invoke(): AppResult<Unit> {
        unregisterCurrentDeviceToken()
        return authRepository.logout()
    }
}
