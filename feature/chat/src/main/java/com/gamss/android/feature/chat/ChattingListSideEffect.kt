package com.gamss.android.feature.chat

/**
 * 문구 대신 종류를 보낸다. 문구를 담으면 사용자 문구가 ViewModel 로 새고 테스트가 한국어 문자열에 묶인다.
 */
sealed interface ChattingListSideEffect {
    data class OpenChatRoom(val conversationId: Long) : ChattingListSideEffect

    data object ShowLoadFailed : ChattingListSideEffect

    data object ShowDeleteSucceeded : ChattingListSideEffect

    data class ShowDeletePartiallyFailed(val failedCount: Int) : ChattingListSideEffect

    data object ShowDeleteFailed : ChattingListSideEffect

    data object ShowSessionExpired : ChattingListSideEffect

    data class ShowSearchFailed(val reason: SearchFailureReason) : ChattingListSideEffect
}

enum class SearchFailureReason {
    INVALID_INPUT,
    NETWORK,
    UNKNOWN,
}
