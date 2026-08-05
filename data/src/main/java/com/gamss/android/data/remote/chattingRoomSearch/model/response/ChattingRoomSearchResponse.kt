package com.gamss.android.data.remote.chattingRoomSearch.model.response

import com.gamss.android.domain.chattingsearch.ChattingRoomSearch
import com.gamss.android.domain.chattingsearch.ChattingRoomSummary
import kotlinx.serialization.Serializable

@Serializable
internal data class ChattingRoomSearchResponse(
    val content: List<ChattingRoomResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

@Serializable
internal data class ChattingRoomResponse(
    val conversationId: Long,
    val title: String,
    val status: String,
    val createdAt: String,
)

internal fun ChattingRoomSearchResponse.toDomain(): ChattingRoomSearch =
    ChattingRoomSearch(
        rooms = content.map { it.toDomain() },
        page = page,
        size = size,
        totalElements = totalElements,
        totalPages = totalPages,
    )

internal fun ChattingRoomResponse.toDomain(): ChattingRoomSummary =
    ChattingRoomSummary(
        conversationId = conversationId,
        title = title,
        status = status,
        createdAt = createdAt,
    )
