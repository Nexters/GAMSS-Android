package com.gamss.android.data.remote.auth

import com.gamss.android.data.remote.auth.model.request.LoginRequest
import com.gamss.android.data.remote.auth.model.request.RefreshTokenRequest
import com.gamss.android.data.remote.auth.model.response.LoginResponse
import com.gamss.android.data.remote.model.response.ApiResponse
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 로그인/토큰 재발급 전용 서비스. Authorization 헤더가 필요 없는 엔드포인트만 모아두며,
 * 인증 API 전용 OkHttpClient([com.gamss.android.data.di.AuthApi])에 바인딩된다.
 * 인증이 필요한 회원 관련 API는 [com.gamss.android.data.remote.user.UserService]를 쓴다.
 */
internal interface AuthService {

    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<LoginResponse>

    @POST("/api/auth/reissue")
    suspend fun reissueTokens(@Body request: RefreshTokenRequest): ApiResponse<LoginResponse>
}
