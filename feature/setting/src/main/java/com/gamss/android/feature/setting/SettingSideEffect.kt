package com.gamss.android.feature.setting

sealed interface SettingSideEffect {
    data object LoadUserInfoFailure : SettingSideEffect
    data object UpdateNicknameSuccess : SettingSideEffect
    data class UpdateNicknameFailure(val reason: NicknameFailureReason) : SettingSideEffect
    data object DeleteAccountFailure : SettingSideEffect
}

enum class NicknameFailureReason {
    MISSING,
    INVALID_LENGTH,
    INVALID_NICKNAME,
    NETWORK,
    UNKNOWN,
}
