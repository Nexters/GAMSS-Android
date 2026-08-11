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

    /** 되돌릴 수 없다. 종료된 방에는 메시지를 추가할 수 없다. */
    suspend fun endConversation(conversationId: Long): AppResult<Unit>
}
