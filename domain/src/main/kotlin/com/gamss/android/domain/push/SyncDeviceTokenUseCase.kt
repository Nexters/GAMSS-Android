package com.gamss.android.domain.push

import com.gamss.android.domain.usecase.NoParamUseCase
import javax.inject.Inject

/**
 * 현재 알림 권한 상태에 맞춰 디바이스 토큰을 등록하거나 해제한다.
 *
 * 앱 실행/포그라운드 진입마다 호출해도 안전하다 — 같은 토큰이면 서버에서 갱신만 된다.
 * 권한이 꺼져 있는데 등록된 토큰을 그대로 두면 서버가 알림을 끈 사용자에게 계속 발송을 시도한다.
 */
class SyncDeviceTokenUseCase @Inject constructor(
    private val pushTokenProvider: PushTokenProvider,
    private val notificationPermissionChecker: NotificationPermissionChecker,
    private val deviceTokenRepository: DeviceTokenRepository,
) : NoParamUseCase<Unit> {

    override suspend fun invoke() {
        val token = pushTokenProvider.getToken() ?: return
        if (notificationPermissionChecker.isGranted()) {
            deviceTokenRepository.registerToken(token)
        } else {
            deviceTokenRepository.unregisterToken(token)
        }
    }
}
