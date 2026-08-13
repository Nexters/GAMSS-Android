package com.gamss.android.feature.setting.nicknamechange

data class NicknameChangeState(
    val originalNickname: String = "",
    val nicknameInput: String = "",
    val isSaving: Boolean = false,
)
