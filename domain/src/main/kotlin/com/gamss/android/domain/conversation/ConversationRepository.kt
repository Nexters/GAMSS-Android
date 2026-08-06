package com.gamss.android.domain.conversation

import com.gamss.android.core.common.AppResult

interface ConversationRepository {

    suspend fun sendMessage(
        conversationId: Long?,
        content: String,
        replyToMessageId: Long?,
        contextSummary: String?,
    ): AppResult<SentMessage>

    suspend fun getMessages(conversationId: Long): AppResult<List<Message>>
}
