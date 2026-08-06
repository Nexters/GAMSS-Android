package com.gamss.android.domain.user

sealed class NicknameUpdateException : RuntimeException() {
    class MissingNickname : NicknameUpdateException()
    class InvalidLength : NicknameUpdateException()
    class InvalidNickname : NicknameUpdateException()
}
