package com.gamss.android.domain.model

sealed class AuthException(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {

    class InvalidCredentials(message: String, cause: Throwable? = null) : AuthException(message, cause)

    class SessionExpired(cause: Throwable? = null) : AuthException("Session expired: refresh token is missing or rejected", cause)

    class Network(cause: Throwable) : AuthException("Network request failed", cause)
}
