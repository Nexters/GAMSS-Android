package com.gamss.android.data.remote.auth.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    @SerialName("idToken")
    val idToken: String,
)
