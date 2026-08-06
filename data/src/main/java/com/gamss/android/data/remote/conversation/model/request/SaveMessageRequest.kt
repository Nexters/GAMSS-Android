package com.gamss.android.data.remote.conversation.model.request

import kotlinx.serialization.Serializable

@Serializable
internal data class SaveMessageRequest(
    val content: String,
    val conversationId: Long? = null,
    val repliesToMessageId: Long? = null,
    val currentConversationSummary: String? = null,
)
