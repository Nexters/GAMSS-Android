package com.gamss.android.domain.user

sealed class NicknameUpdateException(cause: Throwable? = null) : RuntimeException(cause) {
    class MissingNickname(cause: Throwable? = null) : NicknameUpdateException(cause)
    class InvalidLength(cause: Throwable? = null) : NicknameUpdateException(cause)
    class InvalidNickname(cause: Throwable? = null) : NicknameUpdateException(cause)
}
