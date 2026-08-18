package com.gamss.android.domain.user

/**
 * 닉네임 검증 규칙. UseCase와 UI가 함께 참조할 수 있도록
 * UpdateNicknameUseCase가 아닌 별도 객체로 분리해 둔다.
 */
object NicknamePolicy {
    const val MIN_LENGTH = 2
    const val MAX_LENGTH = 10
}
