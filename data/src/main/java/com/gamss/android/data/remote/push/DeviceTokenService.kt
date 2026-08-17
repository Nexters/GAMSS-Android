package com.gamss.android.data.remote.push

import com.gamss.android.data.remote.model.response.ApiResponse
import com.gamss.android.data.remote.push.model.request.RegisterDeviceTokenRequest
import com.gamss.android.data.remote.push.model.request.UnregisterDeviceTokenRequest
import retrofit2.http.Body
import retrofit2.http.HTTP
import retrofit2.http.POST

internal interface DeviceTokenService {

    @POST("/api/members/me/device-tokens")
    suspend fun register(@Body request: RegisterDeviceTokenRequest): ApiResponse<Unit>

    @HTTP(method = "DELETE", path = "/api/members/me/device-tokens", hasBody = true)
    suspend fun unregister(@Body request: UnregisterDeviceTokenRequest): ApiResponse<Unit>
}
