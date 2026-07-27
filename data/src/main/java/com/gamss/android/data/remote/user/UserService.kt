package com.gamss.android.data.remote.user

import com.gamss.android.data.remote.user.model.request.UpdateNicknameRequest
import com.gamss.android.data.remote.user.model.response.SecessionUserResponse
import com.gamss.android.data.remote.user.model.response.UpdateNicknameResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.PATCH

interface UserService {

    @PATCH("/api/memebers/me/nickname")
    suspend fun updateNickname(@Body request: UpdateNicknameRequest): UpdateNicknameResponse

    @DELETE("/api/memebers/me")
    suspend fun secessionUser(): SecessionUserResponse
}
