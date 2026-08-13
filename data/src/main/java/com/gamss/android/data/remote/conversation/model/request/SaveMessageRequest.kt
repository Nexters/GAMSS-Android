package com.gamss.android.data.remote.conversation.model.request

import kotlinx.serialization.Serializable

@Serializable
internal data class SaveMessageRequest(
    val content: String,
    val conversationId: Long? = null,
    val repliesToMessageId: Long? = null,
    val currentConversationSummary: String? = null,
    /** 반응하지 않을 캐릭터. 비어 있으면 보내지 않는다. 6종 전체를 보내면 서버가 INVALID_INPUT 으로 거부한다. */
    val excludeCharacters: List<String>? = null,
)
