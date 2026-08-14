package com.gamss.android.domain.conversation

import androidx.paging.PagingData
import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.conversation.chattingsearch.ChattingRoomSummary
import com.gamss.android.domain.emotion.EmotionCharacter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteConversationUseCaseTest {

    @Test
    fun 리포지토리_성공을_그대로_전한다() = runBlocking {
        val repository = RecordingRepository()

        val result = DeleteConversationUseCase(repository)(7L)

        assertEquals(7L, repository.deletedId)
        assertTrue(result is AppResult.Success)
    }

    @Test
    fun 리포지토리_실패를_그대로_전한다() = runBlocking {
        val repository = RecordingRepository(failing = true)

        val result = DeleteConversationUseCase(repository)(7L)

        assertTrue(result is AppResult.Failure)
    }

    private class RecordingRepository(private val failing: Boolean = false) : ConversationRepository {
        var deletedId: Long? = null
            private set

        override suspend fun deleteConversation(conversationId: Long): AppResult<Unit> {
            deletedId = conversationId
            return if (failing) AppResult.Failure(IllegalStateException("delete failed")) else AppResult.Success(Unit)
        }

        override suspend fun sendMessage(
            conversationId: Long?,
            content: String,
            replyToMessageId: Long?,
            contextSummary: String?,
            excludeCharacters: Set<EmotionCharacter>,
        ): AppResult<SentMessage> = throw UnsupportedOperationException()

        override suspend fun getOngoingConversations(): AppResult<List<Conversation>> =
            throw UnsupportedOperationException()

        override suspend fun getMessages(conversationId: Long): AppResult<List<Message>> =
            throw UnsupportedOperationException()

        override suspend fun updateTitle(conversationId: Long, title: String): AppResult<Unit> =
            throw UnsupportedOperationException()

        override suspend fun endConversation(conversationId: Long): AppResult<Unit> =
            throw UnsupportedOperationException()

        override fun searchChattingRooms(keyword: String): Flow<PagingData<ChattingRoomSummary>> =
            throw UnsupportedOperationException()
    }
}
