package com.gamss.android.domain.push

import com.gamss.android.domain.usecase.NoParamUseCase
import javax.inject.Inject

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
