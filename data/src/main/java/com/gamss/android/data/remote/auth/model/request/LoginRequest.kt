package com.gamss.android.data.remote.auth.model.request

import kotlinx.serialization.Serializable

@Serializable
internal data class LoginRequest(
    val idToken: String,
)
