package com.gamss.android.feature.setting.nicknamechange

sealed interface NicknameChangeSideEffect {
    data object UpdateSuccess : NicknameChangeSideEffect
    data class UpdateFailure(val reason: NicknameFailureReason) : NicknameChangeSideEffect
}

enum class NicknameFailureReason {
    MISSING,
    INVALID_LENGTH,
    INVALID_NICKNAME,
    NETWORK,
    UNKNOWN,
}
