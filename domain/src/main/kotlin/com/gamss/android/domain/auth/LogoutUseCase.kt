package com.gamss.android.domain.auth

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.push.UnregisterCurrentDeviceTokenUseCase
import com.gamss.android.domain.usecase.NoParamUseCase
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val unregisterCurrentDeviceToken: UnregisterCurrentDeviceTokenUseCase,
) : NoParamUseCase<AppResult<Unit>> {

    /** 토큰이 무효화되기 전에 먼저 해제해야 해제 API가 인증을 통과한다. */
    override suspend fun invoke(): AppResult<Unit> {
        unregisterCurrentDeviceToken()
        return authRepository.logout()
    }
}
