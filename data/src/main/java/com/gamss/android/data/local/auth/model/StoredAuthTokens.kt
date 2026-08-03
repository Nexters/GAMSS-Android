package com.gamss.android.data.local.auth.model

internal data class StoredAuthTokens(
    val accessToken: String?,
    val refreshToken: String?,
)
