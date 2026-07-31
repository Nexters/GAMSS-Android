package com.gamss.android.domain.conversation

import com.gamss.android.core.common.AppResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SendMessageUseCaseTest {

    /** 전달받은 인자를 기록하는 fake repository. */
    private class RecordingRepository : ConversationRepository {
        var called = false
            private set
        var sentContent: String? = null
            private set
        var sentConversationId: Long? = null
            private set
        var sentReplyToMessageId: Long? = null
            private set

        override suspend fun sendMessage(
            conversationId: Long?,
            content: String,
            replyToMessageId: Long?,
        ): AppResult<SentMessage> {
            called = true
            sentConversationId = conversationId
            sentContent = content
            sentReplyToMessageId = replyToMessageId
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

        override suspend fun getMessages(conversationId: Long): AppResult<List<Message>> =
            AppResult.Success(emptyList())

        override suspend fun endConversation(conversationId: Long): AppResult<Unit> =
            AppResult.Success(Unit)
    }

    @Test
    fun 앞뒤_공백을_정리한_내용으로_전송한다() = runBlocking {
        val repository = RecordingRepository()

        val result = SendMessageUseCase(repository)(
            conversationId = 7L,
            content = "  오늘 억울한 일이 있었어  ",
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
            conversationId = 7L,
            content = "고마워",
            replyToMessageId = 42L,
        )

        assertEquals(42L, repository.sentReplyToMessageId)
    }

    @Test
    fun 공백만_있는_입력은_서버를_호출하지_않고_실패한다() = runBlocking {
        val repository = RecordingRepository()

        val result = SendMessageUseCase(repository)(conversationId = null, content = "   \n ")

        assertFalse(repository.called)
        assertTrue(result is AppResult.Failure)
    }

    @Test
    fun 최대_길이를_넘는_입력은_서버를_호출하지_않고_실패한다() = runBlocking {
        val repository = RecordingRepository()

        val result = SendMessageUseCase(repository)(
            conversationId = null,
            content = "가".repeat(MAX_MESSAGE_LENGTH + 1),
        )

        assertFalse(repository.called)
        assertTrue(result is AppResult.Failure)
    }

    @Test
    fun 최대_길이와_같은_입력은_전송한다() = runBlocking {
        val repository = RecordingRepository()

        val result = SendMessageUseCase(repository)(
            conversationId = null,
            content = "가".repeat(MAX_MESSAGE_LENGTH),
        )

        assertTrue(repository.called)
        assertTrue(result is AppResult.Success)
    }
}
