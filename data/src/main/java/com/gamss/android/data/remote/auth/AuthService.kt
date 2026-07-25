package com.gamss.android.data.remote.auth

import com.gamss.android.data.remote.auth.model.request.LoginRequest
import com.gamss.android.data.remote.auth.model.request.RefreshTokenRequest
import com.gamss.android.data.remote.auth.model.request.UpdateNicknameRequest
import com.gamss.android.data.remote.auth.model.response.LoginResponse
import com.gamss.android.data.remote.auth.model.response.SecessionUserResponse
import com.gamss.android.data.remote.auth.model.response.UpdateNicknameResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.PATCH
import retrofit2.http.POST

internal const val LOGIN_PATH = "/api/auth/login"
internal const val REISSUE_TOKENS_PATH = "/api/auth/reissue"

interface AuthService {

    @POST(LOGIN_PATH)
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST(REISSUE_TOKENS_PATH)
    suspend fun reissueTokens(@Body request: RefreshTokenRequest): LoginResponse

    @DELETE("/api/memebers/me")
    suspend fun secessionUser(): SecessionUserResponse

    @PATCH("/api/memebers/me/nickname")
    suspend fun updateNickname(@Body request: UpdateNicknameRequest): UpdateNicknameResponse
}
