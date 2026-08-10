package com.gamss.android.feature.setting

import com.gamss.android.domain.user.UserProfile

data class SettingState(
    val isLoading: Boolean = false,
    val userProfile: UserProfile? = null,
    val nicknameInput: String = "",
)
