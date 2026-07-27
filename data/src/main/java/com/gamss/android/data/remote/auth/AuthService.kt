package com.gamss.android.data.remote.auth

import com.gamss.android.data.remote.auth.model.request.LoginRequest
import com.gamss.android.data.remote.auth.model.request.RefreshTokenRequest
import com.gamss.android.data.remote.auth.model.response.LoginResponse
import retrofit2.http.Body
import retrofit2.http.POST

internal const val LOGIN_PATH = "/api/auth/login"
internal const val REISSUE_TOKENS_PATH = "/api/auth/reissue"

/**
 * 로그인/토큰 재발급 전용 서비스. Authorization 헤더가 필요 없는 엔드포인트만 모아두며,
 * 재발급 전용 OkHttpClient([com.gamss.android.data.di.AuthNetwork])에 바인딩된다.
 * 인증이 필요한 회원 관련 API는 [com.gamss.android.data.remote.user.UserService]를 쓴다.
 */
interface AuthService {

    @POST(LOGIN_PATH)
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST(REISSUE_TOKENS_PATH)
    suspend fun reissueTokens(@Body request: RefreshTokenRequest): LoginResponse
}
