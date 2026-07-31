package com.gamss.android.domain.conversation

import com.gamss.android.core.common.AppResult

interface ConversationRepository {

    /** [conversationId] 가 null 이면 서버가 새 채팅방을 만든다. */
    suspend fun sendMessage(
        conversationId: Long?,
        content: String,
        replyToMessageId: Long?,
    ): AppResult<SentMessage>

    /** 작성순으로 온다. 화면이 이 순서에 의존한다. */
    suspend fun getMessages(conversationId: Long): AppResult<List<Message>>

    /** 되돌릴 수 없다. 종료된 방에는 메시지를 추가할 수 없다. */
    suspend fun endConversation(conversationId: Long): AppResult<Unit>
}
