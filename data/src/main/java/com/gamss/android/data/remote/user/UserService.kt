package com.gamss.android.data.remote.user

import com.gamss.android.data.remote.model.response.ApiResponse
import com.gamss.android.data.remote.user.model.request.UpdateNicknameRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.PATCH

internal interface UserService {

    @PATCH("/api/members/me/nickname")
    suspend fun updateNickname(@Body request: UpdateNicknameRequest): ApiResponse<String>

    @DELETE("/api/members/me")
    suspend fun secessionUser(): ApiResponse<String>
}
