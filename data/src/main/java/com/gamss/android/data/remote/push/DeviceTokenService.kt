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

    // 토큰이 접근 로그에 남지 않도록 쿼리 파라미터가 아닌 본문으로 보낸다.
    // @DELETE는 본문을 지원하지 않아 hasBody를 명시하는 @HTTP를 쓴다.
    @HTTP(method = "DELETE", path = "/api/members/me/device-tokens", hasBody = true)
    suspend fun unregister(@Body request: UnregisterDeviceTokenRequest): ApiResponse<Unit>
}
