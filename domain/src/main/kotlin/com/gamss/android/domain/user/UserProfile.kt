package com.gamss.android.domain.user

data class UserProfile(
    val id: Long,
    val email: String,
    val nickname: String,
    val status: String,
    val createdAt: String,
)
