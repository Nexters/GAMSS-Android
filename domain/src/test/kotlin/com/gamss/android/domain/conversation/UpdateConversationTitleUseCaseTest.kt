package com.gamss.android.domain.conversation

import androidx.paging.PagingData
import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.conversation.chattingsearch.ChattingRoomSummary
import com.gamss.android.domain.emotion.EmotionCharacter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateConversationTitleUseCaseTest {

    @Test
    fun 앞뒤_공백을_정리한_제목을_올린다() = runBlocking {
        val repository = RecordingRepository()

        val result = UpdateConversationTitleUseCase(repository)(
            UpdateConversationTitleUseCase.Params(conversationId = 7L, title = "  팀장이 화냈어  "),
        )

        assertEquals(7L to "팀장이 화냈어", repository.updatedTitle)
        assertTrue(result is AppResult.Success)
    }

    @Test
    fun 서버_상한을_넘는_제목은_잘라서_올린다() = runBlocking {
        val repository = RecordingRepository()

        UpdateConversationTitleUseCase(repository)(
            UpdateConversationTitleUseCase.Params(
                conversationId = 7L,
                title = "가".repeat(SERVER_TITLE_LENGTH_LIMIT + 1),
            ),
        )

        assertEquals(SERVER_TITLE_LENGTH_LIMIT, repository.updatedTitle?.second?.length)
    }

    @Test
    fun 서버_상한과_같은_제목은_그대로_올린다() = runBlocking {
        val repository = RecordingRepository()
        val title = "가".repeat(SERVER_TITLE_LENGTH_LIMIT)

        UpdateConversationTitleUseCase(repository)(
            UpdateConversationTitleUseCase.Params(conversationId = 7L, title = title),
        )

        assertEquals(7L to title, repository.updatedTitle)
    }

    @Test
    fun 서버가_거절하면_실패를_그대로_전한다() = runBlocking {
        val repository = RecordingRepository(failing = true)

        val result = UpdateConversationTitleUseCase(repository)(
            UpdateConversationTitleUseCase.Params(conversationId = 7L, title = "팀장이 화냈어"),
        )

        assertTrue(result is AppResult.Failure)
    }

    @Test
    fun 공백만_있는_제목은_서버를_호출하지_않고_실패한다() = runBlocking {
        val repository = RecordingRepository()

        val result = UpdateConversationTitleUseCase(repository)(
            UpdateConversationTitleUseCase.Params(conversationId = 7L, title = "   "),
        )

        assertNull(repository.updatedTitle)
        assertTrue(result is AppResult.Failure)
    }

    private class RecordingRepository(private val failing: Boolean = false) : ConversationRepository {
        var updatedTitle: Pair<Long, String>? = null
            private set

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

        override suspend fun updateTitle(conversationId: Long, title: String): AppResult<Unit> {
            updatedTitle = conversationId to title
            return if (failing) AppResult.Failure(IllegalStateException("rejected")) else AppResult.Success(Unit)
        }

        override suspend fun endConversation(conversationId: Long): AppResult<Unit> =
            throw UnsupportedOperationException()

        override suspend fun deleteConversation(conversationId: Long): AppResult<Unit> =
            throw UnsupportedOperationException()

        override fun searchChattingRooms(keyword: String): Flow<PagingData<ChattingRoomSummary>> =
            throw UnsupportedOperationException()
    }
}
