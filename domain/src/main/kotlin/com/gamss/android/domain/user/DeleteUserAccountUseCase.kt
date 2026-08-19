package com.gamss.android.domain.user

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.auth.AuthRepository
import com.gamss.android.domain.card.ClearCardCacheUseCase
import com.gamss.android.domain.push.UnregisterCurrentDeviceTokenUseCase
import com.gamss.android.domain.usecase.NoParamUseCase
import javax.inject.Inject

class DeleteUserAccountUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val unregisterCurrentDeviceToken: UnregisterCurrentDeviceTokenUseCase,
    private val clearCardCache: ClearCardCacheUseCase,
) : NoParamUseCase<AppResult<Unit>> {

    /**
     * 탈퇴가 끝나면 인증이 무효해져 해제 요청을 보낼 수 없으므로 토큰을 먼저 해제한다.
     *
     * 탈퇴가 실패하면 세션은 그대로 남지만 토큰은 해제된 상태가 된다. 이 경우 포그라운드 복귀나
     * 세션 변화 때 도는 동기화가 다시 등록하므로 복구를 따로 처리하지 않는다.
     */
    override suspend fun invoke(): AppResult<Unit> {
        unregisterCurrentDeviceToken()
        val deleteResult = userRepository.deleteUserAccount()
        if (deleteResult is AppResult.Failure) return deleteResult
        authRepository.logout()
        // 탈퇴가 성공한 뒤에만 비운다. 실패로 일찍 반환된 경우 계정도 캐시도 그대로 둬야 한다.
        clearCardCache()
        return AppResult.Success(Unit)
    }
}
