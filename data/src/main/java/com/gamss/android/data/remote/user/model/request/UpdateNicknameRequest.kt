package com.gamss.android.data.remote.user.model.request

import kotlinx.serialization.Serializable

@Serializable
internal data class UpdateNicknameRequest(
    val nickname: String,
)
