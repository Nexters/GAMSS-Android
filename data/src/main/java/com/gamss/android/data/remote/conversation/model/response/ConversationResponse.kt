package com.gamss.android.data.remote.conversation.model.response

import kotlinx.serialization.Serializable

/** 종료 응답. 필드를 읽지 않으므로 서버가 스키마를 바꿔도 종료가 실패하지 않게 전부 nullable 로 둔다. */
@Serializable
internal data class ConversationResponse(
    val id: Long? = null,
    val status: String? = null,
)
