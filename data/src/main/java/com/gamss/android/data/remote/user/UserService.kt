package com.gamss.android.data.remote.user

import com.gamss.android.data.remote.model.response.ApiResponse
import com.gamss.android.data.remote.user.model.request.UpdateNicknameRequest
import com.gamss.android.data.remote.user.model.response.DailyTokenUsageDataResponse
import com.gamss.android.data.remote.user.model.response.UserInfoResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH

internal interface UserService {

    @PATCH("/api/members/me/nickname")
    suspend fun updateNickname(@Body request: UpdateNicknameRequest): ApiResponse<UserInfoResponse>

    @DELETE("/api/members/me")
    suspend fun deleteUserAccount(): ApiResponse<String>

    @GET("/api/members/me")
    suspend fun getUserInfo(): ApiResponse<UserInfoResponse>

    @GET("/api/members/me/token-usage")
    suspend fun getDailyTokenUsage(): ApiResponse<DailyTokenUsageDataResponse>
}
