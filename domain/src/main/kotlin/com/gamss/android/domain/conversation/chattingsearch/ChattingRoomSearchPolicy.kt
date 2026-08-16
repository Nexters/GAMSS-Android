package com.gamss.android.domain.conversation.chattingsearch

/**
 * 채팅방 검색 키워드 검증 규칙. UseCase와 UI가 함께 참조할 수 있도록
 * SearchChattingRoomsUseCase가 아닌 별도 객체로 분리해 둔다.
 */
object ChattingRoomSearchPolicy {
    const val MIN_KEYWORD_LENGTH = 2
}
