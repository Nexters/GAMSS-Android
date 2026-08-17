package com.gamss.android.domain.conversation.chattingsearch

data class ChattingRoomSearch(
    val rooms: List<ChattingRoomSummary>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    val hasNextPage: Boolean
        get() = page + 1 < totalPages
}

data class ChattingRoomSummary(
    val conversationId: Long,
    val title: String,
    val createdAt: String,
)
