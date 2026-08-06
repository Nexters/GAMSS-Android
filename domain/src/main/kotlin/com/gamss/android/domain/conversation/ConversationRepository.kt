package com.gamss.android.domain.conversation

import com.gamss.android.core.common.AppResult
import java.time.LocalDate

interface ConversationRepository {

    /** 그 날짜(KST)에 만들어진 방만 내려온다. */
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
