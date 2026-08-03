package com.gamss.android.feature.login

sealed interface LoginSideEffect {
    data class ShowToast(val message: String) : LoginSideEffect
}
