package com.gamss.android.domain.conversation.chattingsearch

sealed class ChattingRoomSearchException(cause: Throwable? = null) : RuntimeException(cause) {
    class InvalidKeyword(cause: Throwable? = null) : ChattingRoomSearchException(cause)
}
