package com.gamss.android.domain.conversation

import com.gamss.android.core.common.AppResult

interface ConversationRepository {
    /** 아직 종료하지 않은 방만 최신순으로 온다. 종료·삭제된 방과 카드가 만들어진 방은 빠진다. */
    suspend fun getOngoingConversations(): AppResult<List<Conversation>>

    suspend fun sendMessage(
        conversationId: Long?,
        content: String,
        replyToMessageId: Long?,
        contextSummary: String?,
    ): AppResult<SentMessage>

    suspend fun getMessages(conversationId: Long): AppResult<List<Message>>

    suspend fun updateTitle(conversationId: Long, title: String): AppResult<Unit>

    suspend fun endConversation(conversationId: Long): AppResult<Unit>
}
