package com.gamss.android.domain.conversation

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.summary.DiarySummarizer
import com.gamss.android.domain.summary.UtteranceTokenCounter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ConversationSessionTest {

    @Test
    fun 새_대화의_첫_발화로_제목을_한_번_정한다() = runBlocking {
        val repository = FakeConversationRepository()
        val session = session(repository)

        session.openWith(SEED)
        session.continueWith("팀장이 또 그랬어")

        assertEquals(listOf(ROOM_ID to TITLE), repository.updatedTitles)
    }

    @Test
    fun 제목_지정은_전송_왕복에_끼지_않는다() = runBlocking {
        val repository = FakeConversationRepository()
        val session = session(repository)

        session.send(conversationId = null, content = SEED, replyToMessageId = null)

        assertTrue(repository.updatedTitles.isEmpty())

        session.finishSend()

        assertEquals(listOf(ROOM_ID to TITLE), repository.updatedTitles)
    }

    @Test
    fun 이어_쓰는_대화는_제목을_건드리지_않는다() = runBlocking {
        val repository = FakeConversationRepository()
        val session = session(repository)

        session.continueWith(SEED)

        assertTrue(repository.updatedTitles.isEmpty())
    }

    @Test
    fun 전송이_실패하면_제목을_시도하지_않는다() = runBlocking {
        val repository = FakeConversationRepository(failingSend = true)
        val session = session(repository)

        session.openWith(SEED)

        assertTrue(repository.updatedTitles.isEmpty())
    }

    @Test
    fun 제목_지정이_실패해도_전송은_성공이고_다음_뒷정리에서_다시_시도한다() = runBlocking {
        val repository = FakeConversationRepository(failingTitle = true)
        val session = session(repository)

        val result = session.send(conversationId = null, content = SEED, replyToMessageId = null)
        session.finishSend()
        repository.failingTitle = false
        session.continueWith("팀장이 또 그랬어")

        assertTrue(result is AppResult.Success)
        assertEquals(listOf(ROOM_ID to TITLE, ROOM_ID to TITLE), repository.updatedTitles)
    }

    @Test
    fun 불러온_대화는_올리지_못한_제목을_버린다() = runBlocking {
        val repository = FakeConversationRepository(failingTitle = true)
        val session = session(repository)

        session.openWith(SEED)
        repository.failingTitle = false
        session.restore(OTHER_ROOM_ID)
        session.continueWith("다른 방에 남기는 말")

        assertEquals(1, repository.updatedTitles.size)
    }

    @Test
    fun 제목으로_쓸_글자가_없으면_지정을_시도하지_않는다() = runBlocking {
        val repository = FakeConversationRepository()
        val session = session(repository)

        session.openWith("...")

        assertTrue(repository.updatedTitles.isEmpty())
    }

    @Test
    fun 긴_시드는_잘린_제목으로_올라간다() = runBlocking {
        val repository = FakeConversationRepository()
        val session = session(repository)

        session.openWith("가".repeat(MAX_MESSAGE_LENGTH))

        assertEquals(
            listOf(ROOM_ID to "가".repeat(MAX_CONVERSATION_TITLE_LENGTH - 1) + "…"),
            repository.updatedTitles,
        )
    }

    /** 새 대화를 열고 뒷정리까지 끝낸다. 화면(ChatRoomViewModel)이 하는 순서와 같다. */
    private suspend fun ConversationSession.openWith(content: String) {
        send(conversationId = null, content = content, replyToMessageId = null)
        finishSend()
    }

    private suspend fun ConversationSession.continueWith(content: String) {
        send(conversationId = ROOM_ID, content = content, replyToMessageId = null)
        finishSend()
    }

    private fun session(repository: FakeConversationRepository) = ConversationSession(
        sendMessage = SendMessageUseCase(repository),
        getMessages = GetMessagesUseCase(repository),
        updateConversationTitle = UpdateConversationTitleUseCase(repository),
        summaryStore = ConversationSummaryStore(
            summarizer = PassThroughSummarizer,
            tokenCounter = CharLengthTokenCounter,
        ),
    )

    private object PassThroughSummarizer : DiarySummarizer {
        override suspend fun summarize(text: String): String = text
    }

    private object CharLengthTokenCounter : UtteranceTokenCounter {
        override suspend fun count(text: String): Int = text.length
    }

    private class FakeConversationRepository(
        var failingTitle: Boolean = false,
        private val failingSend: Boolean = false,
    ) : ConversationRepository {

        val updatedTitles = mutableListOf<Pair<Long, String>>()

        override suspend fun sendMessage(
            conversationId: Long?,
            content: String,
            replyToMessageId: Long?,
            contextSummary: String?,
        ): AppResult<SentMessage> {
            if (failingSend) return AppResult.Failure(IllegalStateException("send failed"))
            return AppResult.Success(
                SentMessage(
                    message = Message(
                        id = MESSAGE_ID,
                        conversationId = conversationId ?: ROOM_ID,
                        sender = MessageSender.User,
                        content = content,
                    ),
                    commentStatus = CommentGenerationStatus.DONE,
                    comments = emptyList(),
                ),
            )
        }

        override suspend fun getConversations(date: LocalDate): AppResult<List<Conversation>> =
            AppResult.Success(emptyList())

        override suspend fun getMessages(conversationId: Long): AppResult<List<Message>> =
            AppResult.Success(emptyList())

        override suspend fun updateTitle(conversationId: Long, title: String): AppResult<Unit> {
            updatedTitles += conversationId to title
            return if (failingTitle) {
                AppResult.Failure(IllegalStateException("update title failed"))
            } else {
                AppResult.Success(Unit)
            }
        }
    }

    private companion object {
        /** 정책이 실제로 걷어내는 시드라야 세션과 정책이 물려 있는지 검증된다. */
        const val SEED = "아 진짜 짜증나 팀장이 아이디어 가로챘어"
        const val TITLE = "팀장이 아이디어 가로챘어"
        const val ROOM_ID = 7L
        const val OTHER_ROOM_ID = 8L
        const val MESSAGE_ID = 100L
    }
}
