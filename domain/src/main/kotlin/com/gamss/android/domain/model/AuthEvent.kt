package com.gamss.android.domain.model

sealed interface AuthEvent {
    data object SessionExpired : AuthEvent
    data object LoggedOut : AuthEvent
}
