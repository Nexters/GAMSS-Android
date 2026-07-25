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
)

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
