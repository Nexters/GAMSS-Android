package com.gamss.android.data.remote.conversation.model.request

import kotlinx.serialization.Serializable

/**
 * null 필드는 직렬화에서 빠져야 한다(conversationId 생략 = 새 채팅방). Json.encodeDefaults=false 전제.
 *
 * @param currentConversationSummary 서버가 저장하지 않고 생성 컨텍스트로만 쓰는 압축본.
 */
@Serializable
internal data class SaveMessageRequest(
    val content: String,
    val conversationId: Long? = null,
    val repliesToMessageId: Long? = null,
    val currentConversationSummary: String? = null,
)
