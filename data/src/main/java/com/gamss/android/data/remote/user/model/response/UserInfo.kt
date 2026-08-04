package com.gamss.android.data.remote.user.model.response

import com.gamss.android.domain.user.UserProfile
import kotlinx.serialization.Serializable

@Serializable
data class UserInfo(
    val id: Long,
    val email: String,
    val nickname: String,
    val status: String,
    val createdAt: String,
)

internal fun UserInfo.toDomain(): UserProfile =
    UserProfile(
        id = id,
        email = email,
        nickname = nickname,
        status = status,
        createdAt = createdAt,
    )