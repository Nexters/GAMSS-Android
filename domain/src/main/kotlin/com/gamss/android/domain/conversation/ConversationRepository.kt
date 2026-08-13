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

    /**
     * 되돌릴 수 없다. 이미 삭제된 방을 다시 지우는 것은 성공으로 본다.
     * 취소는 [AppResult.Failure] 에 담지 않고 던진다. 값으로 담으면 호출부가 삭제 실패로 보고한다.
     */
    suspend fun deleteConversation(conversationId: Long): AppResult<Unit>
}
