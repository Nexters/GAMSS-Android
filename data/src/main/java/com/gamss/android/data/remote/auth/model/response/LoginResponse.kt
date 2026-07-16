package com.gamss.android.data.remote.auth.model.response

import com.gamss.android.domain.model.AuthResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    @SerialName("accessToken")
    val accessToken: String,
    @SerialName("refreshToken")
    val refreshToken: String,
) {
    fun toDomain() = AuthResponse(
        accessToken = accessToken,
        refreshToken = refreshToken
    )
}