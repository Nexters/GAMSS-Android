package com.gamss.android.domain.auth

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.card.ClearCardCacheUseCase
import com.gamss.android.domain.push.UnregisterCurrentDeviceTokenUseCase
import com.gamss.android.domain.usecase.NoParamUseCase
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val unregisterCurrentDeviceToken: UnregisterCurrentDeviceTokenUseCase,
    private val clearCardCache: ClearCardCacheUseCase,
) : NoParamUseCase<AppResult<Unit>> {

    override suspend fun invoke(): AppResult<Unit> {
        unregisterCurrentDeviceToken()
        val result = authRepository.logout()
        // authRepository.logout() 은 결과와 무관하게 세션을 Unauthenticated 로 내린다.
        // 다음에 이 기기에서 다른 계정이 로그인할 수 있으므로 캐시도 결과와 상관없이 비운다.
        clearCardCache()
        return result
    }
}
