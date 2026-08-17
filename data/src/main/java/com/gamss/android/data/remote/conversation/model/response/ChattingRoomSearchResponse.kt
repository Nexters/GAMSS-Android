package com.gamss.android.data.remote.conversation.model.response

import com.gamss.android.domain.conversation.chattingsearch.ChattingRoomSearch
import com.gamss.android.domain.conversation.chattingsearch.ChattingRoomSummary
import kotlinx.serialization.Serializable

@Serializable
internal data class ChattingRoomSearchResponse(
    val content: List<ChattingRoomResponse> = emptyList(),
    val page: Int = 0,
    val size: Int = 0,
    val totalElements: Long = 0L,
    val totalPages: Int = 0,
)

@Serializable
internal data class ChattingRoomResponse(
    val conversationId: Long? = null,
    val title: String? = null,
    val status: String? = null,
    val createdAt: String? = null,
)

internal fun ChattingRoomSearchResponse.toDomain(): ChattingRoomSearch =
    ChattingRoomSearch(
        rooms = content.mapNotNull { it.toDomain() },
        page = page,
        size = size,
        totalElements = totalElements,
        totalPages = totalPages,
    )

internal fun ChattingRoomResponse.toDomain(): ChattingRoomSummary? {
    val id = conversationId ?: return null
    return ChattingRoomSummary(
        conversationId = id,
        title = title.orEmpty(),
        createdAt = createdAt.orEmpty(),
    )
}
