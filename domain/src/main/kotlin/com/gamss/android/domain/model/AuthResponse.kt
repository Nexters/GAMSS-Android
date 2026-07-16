package com.gamss.android.domain.model

data class AuthResponse(
    val accessToken: String = "",
    val refreshToken: String = "",
)
