package com.gamss.android.data.remote.user.model.request

import kotlinx.serialization.Serializable

@Serializable
data class UpdateNicknameRequest(
    val nickname: String,
)
