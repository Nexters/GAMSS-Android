package com.gamss.android.data.remote.conversation.model.request

import kotlinx.serialization.Serializable

/** null 필드는 직렬화에서 빠져야 한다(conversationId 생략 = 새 채팅방). Json.encodeDefaults=false 전제. */
@Serializable
internal data class SaveMessageRequest(
    val content: String,
    val conversationId: Long? = null,
    val repliesToMessageId: Long? = null,
)
