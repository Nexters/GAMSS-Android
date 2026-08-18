package com.gamss.android.domain.conversation

import androidx.paging.PagingData
import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.conversation.chattingsearch.ChattingRoomSummary
import com.gamss.android.domain.emotion.EmotionCharacter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SendMessageUseCaseTest {

    private class RecordingRepository : ConversationRepository {
        var called = false
            private set
        var sentContent: String? = null
            private set
        var sentConversationId: Long? = null
            private set
        var sentReplyToMessageId: Long? = null
            private set
        var sentContextSummary: String? = null
            private set

        var sentExcludeCharacters: Set<EmotionCharacter> = emptySet()
            private set

        override suspend fun sendMessage(
            conversationId: Long?,
            content: String,
            replyToMessageId: Long?,
            contextSummary: String?,
            excludeCharacters: Set<EmotionCharacter>,
        ): AppResult<SentMessage> {
            called = true
            sentExcludeCharacters = excludeCharacters
            sentConversationId = conversationId
            sentContent = content
            sentReplyToMessageId = replyToMessageId
            sentContextSummary = contextSummary
            return AppResult.Success(
                SentMessage(
                    message = Message(
                        id = 1L,
                        conversationId = conversationId ?: 10L,
                        sender = MessageSender.User,
                        content = content,
                        repliesToMessageId = replyToMessageId,
                    ),
                    commentStatus = CommentGenerationStatus.DONE,
                    comments = emptyList(),
                ),
            )
        }

        override suspend fun getOngoingConversations(): AppResult<List<Conversation>> =
            AppResult.Success(emptyList())

        override suspend fun getMessages(conversationId: Long): AppResult<List<Message>> =
            AppResult.Success(emptyList())

        override suspend fun updateTitle(conversationId: Long, title: String): AppResult<Unit> =
            AppResult.Success(Unit)

        override suspend fun endConversation(conversationId: Long): AppResult<Unit> =
            AppResult.Success(Unit)

        override suspend fun deleteConversation(conversationId: Long): AppResult<Unit> =
            AppResult.Success(Unit)

        override fun searchChattingRooms(keyword: String): Flow<PagingData<ChattingRoomSummary>> =
            throw UnsupportedOperationException()
    }

    @Test
    fun 새_대화일_때만_제외할_캐릭터를_싣는다() = runBlocking {
        val repository = RecordingRepository()
        val excluded = setOf(EmotionCharacter.SADNESS, EmotionCharacter.ANXIETY)

        SendMessageUseCase(repository)(
            SendMessageUseCase.Params(conversationId = null, content = "새 대화", excludeCharacters = excluded),
        )

        assertEquals(excluded, repository.sentExcludeCharacters)
    }

    @Test
    fun 이어지는_대화에는_제외할_캐릭터를_싣지_않는다() = runBlocking {
        val repository = RecordingRepository()

        SendMessageUseCase(repository)(
            SendMessageUseCase.Params(
                conversationId = 7L,
                content = "이어서",
                excludeCharacters = setOf(EmotionCharacter.SADNESS),
            ),
        )

        assertTrue(repository.sentExcludeCharacters.isEmpty())
    }

    @Test
    fun 여섯_종을_모두_제외하면_보내지_않는다() = runBlocking {
        val repository = RecordingRepository()

        val result = SendMessageUseCase(repository)(
            SendMessageUseCase.Params(
                conversationId = null,
                content = "전부 제외",
                excludeCharacters = EmotionCharacter.entries.toSet(),
            ),
        )

        assertTrue(result is AppResult.Failure)
        assertFalse(repository.called)
    }

    @Test
    fun 다섯_종까지는_제외할_수_있다() = runBlocking {
        val repository = RecordingRepository()
        val fiveOfSix = EmotionCharacter.entries.take(5).toSet()

        val result = SendMessageUseCase(repository)(
            SendMessageUseCase.Params(
                conversationId = null,
                content = "다섯 제외",
                excludeCharacters = fiveOfSix,
            ),
        )

        assertTrue(result is AppResult.Success)
        assertEquals(fiveOfSix, repository.sentExcludeCharacters)
    }

    @Test
    fun 앞뒤_공백을_정리한_내용으로_전송한다() = runBlocking {
        val repository = RecordingRepository()

        val result = SendMessageUseCase(repository)(
            SendMessageUseCase.Params(conversationId = 7L, content = "  오늘 억울한 일이 있었어  "),
        )

        assertTrue(repository.called)
        assertEquals("오늘 억울한 일이 있었어", repository.sentContent)
        assertEquals(7L, repository.sentConversationId)
        assertNull(repository.sentReplyToMessageId)
        assertTrue(result is AppResult.Success)
    }

    @Test
    fun 답장_대상을_그대로_전달한다() = runBlocking {
        val repository = RecordingRepository()

        SendMessageUseCase(repository)(
            SendMessageUseCase.Params(conversationId = 7L, content = "고마워", replyToMessageId = 42L),
        )

        assertEquals(42L, repository.sentReplyToMessageId)
    }

    @Test
    fun 공백만_있는_입력은_서버를_호출하지_않고_실패한다() = runBlocking {
        val repository = RecordingRepository()

        val result = SendMessageUseCase(repository)(
            SendMessageUseCase.Params(conversationId = null, content = "   \n "),
        )

        assertFalse(repository.called)
        assertTrue(result is AppResult.Failure)
    }

    @Test
    fun 최대_길이를_넘는_입력은_서버를_호출하지_않고_실패한다() = runBlocking {
        val repository = RecordingRepository()

        val result = SendMessageUseCase(repository)(
            SendMessageUseCase.Params(conversationId = null, content = "가".repeat(MAX_MESSAGE_LENGTH + 1)),
        )

        assertFalse(repository.called)
        assertTrue(result is AppResult.Failure)
    }

    @Test
    fun 최대_길이와_같은_입력은_전송한다() = runBlocking {
        val repository = RecordingRepository()

        val result = SendMessageUseCase(repository)(
            SendMessageUseCase.Params(conversationId = null, content = "가".repeat(MAX_MESSAGE_LENGTH)),
        )

        assertTrue(repository.called)
        assertTrue(result is AppResult.Success)
    }
}
