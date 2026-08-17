package com.gamss.android.domain.push

import com.gamss.android.domain.usecase.NoParamUseCase
import javax.inject.Inject

/**
 * 로그아웃 시 이 기기로의 알림 발송을 멈추기 위해 호출한다.
 *
 * 해제 실패가 로그아웃을 막으면 안 되므로 결과를 확인하지 않는다 — 이미 해제된 토큰이어도
 * 서버는 200으로 응답하고, 실패해도 다음 [SyncDeviceTokenUseCase] 호출에서 다시 정리된다.
 */
class UnregisterCurrentDeviceTokenUseCase @Inject constructor(
    private val pushTokenProvider: PushTokenProvider,
    private val deviceTokenRepository: DeviceTokenRepository,
) : NoParamUseCase<Unit> {

    override suspend fun invoke() {
        val token = pushTokenProvider.getToken() ?: return
        deviceTokenRepository.unregisterToken(token)
    }
}
