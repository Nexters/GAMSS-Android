package com.gamss.android.data.remote.user.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateNicknameResponse(
    @SerialName("success")
    val success: Boolean,
    @SerialName("data")
    val data: String? = null,
    @SerialName("error")
    val error: UpdateNicknameError? = null,
)

@Serializable
data class UpdateNicknameError(
    @SerialName("code")
    val code: String,
    @SerialName("message")
    val message: String,
)
