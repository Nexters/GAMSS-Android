package com.gamss.android.feature.setting.accountinfo

sealed interface AccountInfoSideEffect {
    data object LoadUserInfoFailure : AccountInfoSideEffect
    data object DeleteAccountFailure : AccountInfoSideEffect
    data object LogoutFailure : AccountInfoSideEffect
}
