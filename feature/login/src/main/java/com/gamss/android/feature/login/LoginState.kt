package com.gamss.android.feature.login

data class LoginState(
    val isLoading: Boolean = false,
    val googleSignInRequestId: Long? = null,
)
