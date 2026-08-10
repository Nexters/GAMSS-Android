package com.gamss.android.data.remote.user.model.response

import com.gamss.android.domain.user.UserProfile
import kotlinx.serialization.Serializable

@Serializable
internal data class UserInfoResponse(
    val id: Long,
    val email: String? = null,
    val nickname: String? = null,
    val status: String? = null,
    val createdAt: String? = null,
)

internal fun UserInfoResponse.toDomain(): UserProfile =
    UserProfile(
        id = id,
        email = email,
        nickname = nickname,
        status = status,
        createdAt = createdAt,
    )
