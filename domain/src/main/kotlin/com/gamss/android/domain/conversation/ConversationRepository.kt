package com.gamss.android.domain.conversation

import com.gamss.android.core.common.AppResult
import java.time.LocalDate

interface ConversationRepository {
    suspend fun getConversations(date: LocalDate): AppResult<List<Conversation>>

    suspend fun sendMessage(
        conversationId: Long?,
        content: String,
        replyToMessageId: Long?,
        contextSummary: String?,
    ): AppResult<SentMessage>

    suspend fun getMessages(conversationId: Long): AppResult<List<Message>>

    suspend fun updateTitle(conversationId: Long, title: String): AppResult<Unit>
}
