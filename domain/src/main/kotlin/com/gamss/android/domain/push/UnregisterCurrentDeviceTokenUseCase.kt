package com.gamss.android.domain.push

import com.gamss.android.domain.usecase.NoParamUseCase
import javax.inject.Inject

class UnregisterCurrentDeviceTokenUseCase @Inject constructor(
    private val pushTokenProvider: PushTokenProvider,
    private val deviceTokenRepository: DeviceTokenRepository,
) : NoParamUseCase<Unit> {

    override suspend fun invoke() {
        val token = pushTokenProvider.getToken() ?: return
        deviceTokenRepository.unregisterToken(token)
    }
}
