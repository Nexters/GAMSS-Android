package com.gamss.android.feature.setting.accountinfo

import com.gamss.android.domain.user.UserProfile

data class AccountInfoState(
    val isLoading: Boolean = false,
    val userProfile: UserProfile? = null,
)
