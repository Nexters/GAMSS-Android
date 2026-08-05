package com.gamss.android.feature.setting

sealed interface SettingSideEffect {
    data object LoadUserInfoFailure : SettingSideEffect
    data object UpdateNicknameSuccess : SettingSideEffect
    data class UpdateNicknameFailure(val throwable: Throwable) : SettingSideEffect
    data object SecessionFailure : SettingSideEffect
}
