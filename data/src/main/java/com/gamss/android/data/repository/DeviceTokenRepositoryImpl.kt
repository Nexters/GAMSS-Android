package com.gamss.android.data.repository

import com.gamss.android.core.common.AppResult
import com.gamss.android.data.remote.push.DeviceTokenService
import com.gamss.android.data.remote.push.model.request.RegisterDeviceTokenRequest
import com.gamss.android.data.remote.push.model.request.UnregisterDeviceTokenRequest
import com.gamss.android.data.remote.runCatchingApiCall
import com.gamss.android.domain.push.DeviceTokenRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DeviceTokenRepositoryImpl @Inject constructor(
    private val deviceTokenService: DeviceTokenService,
) : DeviceTokenRepository {

    override suspend fun registerToken(token: String): AppResult<Unit> = runCatchingApiCall {
        deviceTokenService.register(RegisterDeviceTokenRequest(token = token))
    }

    override suspend fun unregisterToken(token: String): AppResult<Unit> = runCatchingApiCall {
        deviceTokenService.unregister(UnregisterDeviceTokenRequest(token = token))
    }
}
