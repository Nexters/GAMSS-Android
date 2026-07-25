package com.gamss.android.data.remote.auth.model.response

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
    internal fun requireTokenData(): LoginData {
        if (!success) {
            throw AuthRequestException(
                code = error?.code,
                message = error?.message ?: "인증 요청에 실패했습니다.",
            )
        }

        return checkNotNull(data) {
            "성공한 인증 응답에 토큰 데이터가 없습니다."
        }
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

internal class AuthRequestException(
    val code: String?,
    override val message: String,
) : RuntimeException(message)
