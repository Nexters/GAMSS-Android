package com.gamss.android.domain.conversation

import androidx.paging.PagingData
import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.conversation.chattingsearch.ChattingRoomSummary
import com.gamss.android.domain.emotion.EmotionCharacter
import kotlinx.coroutines.flow.Flow

interface ConversationRepository {
    /** 아직 종료하지 않은 방만 최신순으로 온다. 종료·삭제된 방과 카드가 만들어진 방은 빠진다. */
    suspend fun getOngoingConversations(): AppResult<List<Conversation>>

    /**
     * @param excludeCharacters 반응하지 않을 캐릭터. 새 채팅방을 열 때만 적용되고, 이어 보내는 요청에서는
     *  서버가 무시한다. 6종 전체를 넘기면 서버가 거부한다.
     */
    suspend fun sendMessage(
        conversationId: Long?,
        content: String,
        replyToMessageId: Long?,
        contextSummary: String?,
        excludeCharacters: Set<EmotionCharacter>,
    ): AppResult<SentMessage>

    suspend fun getMessages(conversationId: Long): AppResult<List<Message>>

    suspend fun updateTitle(conversationId: Long, title: String): AppResult<Unit>

    suspend fun endConversation(conversationId: Long): AppResult<Unit>

    /**
     * 되돌릴 수 없다. 이미 삭제된 방을 다시 지우는 것은 성공으로 본다.
     * 취소는 [AppResult.Failure] 에 담지 않고 던진다. 값으로 담으면 호출부가 삭제 실패로 보고한다.
     */
    suspend fun deleteConversation(conversationId: Long): AppResult<Unit>

    fun searchChattingRooms(keyword: String): Flow<PagingData<ChattingRoomSummary>>
}
