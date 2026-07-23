package com.gamss.android.data.remote.auth.model.response

import com.gamss.android.domain.model.AuthResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    @SerialName("success")
    val success: Boolean,
    @SerialName("data")
    val data: LoginData? = null,
    @SerialName("error")
    val error: LoginError? = null,
) {
    fun toDomain(): AuthResponse {
        check(success) { error?.message ?: "로그인 요청에 실패했습니다." }
        val loginData = checkNotNull(data) { "로그인 응답에 토큰이 없습니다." }

        return AuthResponse(
            accessToken = loginData.accessToken,
            refreshToken = loginData.refreshToken,
        )
    }
}

@Serializable
data class LoginData(
    @SerialName("accessToken")
    val accessToken: String,
    @SerialName("refreshToken")
    val refreshToken: String,
)

@Serializable
data class LoginError(
    @SerialName("code")
    val code: String,
    @SerialName("message")
    val message: String,
)
