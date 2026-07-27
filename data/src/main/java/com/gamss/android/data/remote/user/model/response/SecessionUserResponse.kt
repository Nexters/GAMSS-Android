package com.gamss.android.data.remote.user.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SecessionUserResponse(
    @SerialName("success")
    val success: Boolean,
    @SerialName("data")
    val data: String? = null,
    @SerialName("error")
    val error: SecessionUserError? = null,
)

@Serializable
data class SecessionUserError(
    @SerialName("code")
    val code: String,
    @SerialName("message")
    val message: String,
)
