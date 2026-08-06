package com.gamss.android.feature.chat

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.conversation.CommentGenerationStatus
import com.gamss.android.domain.conversation.Conversation
import com.gamss.android.domain.conversation.ConversationRepository
import com.gamss.android.domain.conversation.ConversationSession
import com.gamss.android.domain.conversation.ConversationSummaryStore
import com.gamss.android.domain.conversation.GetMessagesUseCase
import com.gamss.android.domain.conversation.Message
import com.gamss.android.domain.conversation.MessageSender
import com.gamss.android.domain.conversation.SendMessageUseCase
import com.gamss.android.domain.conversation.SentMessage
import com.gamss.android.domain.conversation.UpdateConversationTitleUseCase
import com.gamss.android.domain.emotion.EmotionCharacter
import com.gamss.android.domain.summary.DiarySummarizer
import com.gamss.android.domain.summary.UtteranceTokenCounter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.orbitmvi.orbit.test.test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ChatRoomRevealTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun 첫_댓글만_즉시_붙고_나머지는_하나씩_노출된다() = runTest {
        val viewModel = viewModel(commentCount = 3)

        viewModel.test(this) {
            containerHost.onInputChange(INPUT)
            skipItems(1) // input 반영

            containerHost.onSend()
            skipItems(1) // isSending = true

            val afterSend = awaitState()
            assertEquals(listOf(USER_ID, COMMENT_ID_BASE + 0), afterSend.messages.map { it.id })
            assertEquals(listOf(COMMENT_ID_BASE + 1, COMMENT_ID_BASE + 2), afterSend.pendingComments.map { it.id })
            assertTrue(afterSend.isAwaitingComments)

            val firstReveal = awaitState()
            assertEquals(COMMENT_ID_BASE + 1, firstReveal.messages.last().id)
            assertEquals(1, firstReveal.pendingComments.size)

            val secondReveal = awaitState()
            assertEquals(COMMENT_ID_BASE + 2, secondReveal.messages.last().id)
            assertTrue(secondReveal.pendingComments.isEmpty())
            assertFalse(secondReveal.isAwaitingComments)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 노출_중_새로_보내면_남은_댓글이_한꺼번에_붙는다() = runTest {
        val viewModel = viewModel(commentCount = 4)

        viewModel.test(this) {
            containerHost.onInputChange(INPUT)
            skipItems(1) // input 반영
            containerHost.onSend()
            skipItems(1) // isSending = true

            val afterSend = awaitState()
            assertEquals(3, afterSend.pendingComments.size)

            containerHost.onInputChange(INPUT)
            skipItems(1) // input 반영
            containerHost.onSend()

            val flushed = awaitState()
            assertTrue(flushed.pendingComments.isEmpty())
            assertEquals(5, flushed.messages.size)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 댓글이_하나면_노출_대기가_없다() = runTest {
        val viewModel = viewModel(commentCount = 1)

        viewModel.test(this) {
            containerHost.onInputChange(INPUT)
            skipItems(1) // input 반영
            containerHost.onSend()
            skipItems(1) // isSending = true

            val afterSend = awaitState()
            assertTrue(afterSend.pendingComments.isEmpty())
            assertEquals(2, afterSend.messages.size)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 첫_전송에는_압축본이_없고_다음_전송에_직전_발화가_실린다() = runTest {
        val repository = FakeConversationRepository(commentCount = 1)
        val viewModel = viewModel(repository)

        viewModel.test(this) {
            containerHost.onInputChange(INPUT)
            skipItems(1) // input 반영
            containerHost.onSend()
            skipItems(1) // isSending = true
            awaitState() // 전송 성공 반영

            containerHost.onInputChange(SECOND_INPUT)
            skipItems(1) // input 반영
            containerHost.onSend()
            skipItems(1) // isSending = true
            awaitState() // 전송 성공 반영

            assertEquals(listOf(null, INPUT), repository.sentContextSummaries)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 전송이_실패하면_압축본에_쌓이지_않는다() = runTest {
        val repository = FakeConversationRepository(commentCount = 1, failing = true)
        val viewModel = viewModel(repository)

        // 상태와 side effect 가 한 스트림으로 합쳐지므로 타입을 고정해 받는다.
        viewModel.test(this) {
            containerHost.onInputChange(INPUT)
            awaitState()
            containerHost.onSend()
            awaitState()
            awaitState()
            expectSideEffect(ChatRoomSideEffect.ShowToast(SEND_FAILED_MESSAGE))

            containerHost.onSend()
            awaitState()
            awaitState()
            expectSideEffect(ChatRoomSideEffect.ShowToast(SEND_FAILED_MESSAGE))

            assertEquals(listOf(null, null), repository.sentContextSummaries)
            expectNoItems()
        }
    }

    @Test
    fun 새_대화의_첫_전송_뒤_제목이_지정된다() = runTest {
        val repository = FakeConversationRepository(commentCount = 1)
        val viewModel = viewModel(repository)

        viewModel.test(this) {
            containerHost.onInputChange(SEED_INPUT)
            skipItems(1) // input 반영
            containerHost.onSend()
            skipItems(1) // isSending = true
            awaitState() // 전송 성공 반영

            assertEquals(listOf(ROOM_ID to "지갑 잃어버렸어"), repository.updatedTitles)

            cancelAndIgnoreRemainingItems()
        }
    }

    private fun viewModel(commentCount: Int): ChatRoomViewModel =
        viewModel(FakeConversationRepository(commentCount))

    private fun viewModel(conversationRepository: FakeConversationRepository): ChatRoomViewModel {
        return ChatRoomViewModel(
            session = ConversationSession(
                sendMessage = SendMessageUseCase(conversationRepository),
                getMessages = GetMessagesUseCase(conversationRepository),
                updateConversationTitle = UpdateConversationTitleUseCase(conversationRepository),
                summaryStore = ConversationSummaryStore(
                    summarizer = PassThroughSummarizer,
                    tokenCounter = CharLengthTokenCounter,
                ),
            ),
        )
    }

    private object PassThroughSummarizer : DiarySummarizer {
        override suspend fun summarize(text: String): String = text
    }

    private object CharLengthTokenCounter : UtteranceTokenCounter {
        override suspend fun count(text: String): Int = text.length
    }

    private class FakeConversationRepository(
        private val commentCount: Int,
        private val failing: Boolean = false,
    ) : ConversationRepository {
        private var sentCount = 0

        val sentContextSummaries = mutableListOf<String?>()

        val updatedTitles = mutableListOf<Pair<Long, String>>()

        override suspend fun sendMessage(
            conversationId: Long?,
            content: String,
            replyToMessageId: Long?,
            contextSummary: String?,
        ): AppResult<SentMessage> {
            sentContextSummaries += contextSummary
            if (failing) return AppResult.Failure(IllegalStateException("send failed"))
            val roomId = conversationId ?: ROOM_ID
            return AppResult.Success(
                SentMessage(
                    message = message(
                        id = USER_ID + sentCount++,
                        conversationId = roomId,
                        sender = MessageSender.User,
                        content = content,
                    ),
                    commentStatus = CommentGenerationStatus.DONE,
                    comments = List(commentCount) { index ->
                        message(
                            id = COMMENT_ID_BASE + index + (sentCount - 1) * COMMENT_ID_STRIDE,
                            conversationId = roomId,
                            sender = MessageSender.Character(EmotionCharacter.ANGER),
                            content = "댓글 $index",
                        )
                    },
                ),
            )
        }

        override suspend fun getConversations(date: LocalDate): AppResult<List<Conversation>> =
            AppResult.Success(emptyList())

        override suspend fun getMessages(conversationId: Long): AppResult<List<Message>> =
            AppResult.Success(emptyList())

        override suspend fun updateTitle(conversationId: Long, title: String): AppResult<Unit> {
            updatedTitles += conversationId to title
            return AppResult.Success(Unit)
        }
    }

    private companion object {
        const val INPUT = "오늘 억울한 일이 있었어"
        const val SEED_INPUT = "아 진짜 짜증나 지갑 잃어버렸어"
        const val SEND_FAILED_MESSAGE = "메시지를 보내지 못했어요"
        const val SECOND_INPUT = "팀장이 갑자기 일을 더 줬어"
        const val ROOM_ID = 7L
        const val USER_ID = 100L
        const val COMMENT_ID_BASE = 200L
        const val COMMENT_ID_STRIDE = 10L

        fun message(id: Long, conversationId: Long, sender: MessageSender, content: String) = Message(
            id = id,
            conversationId = conversationId,
            sender = sender,
            content = content,
        )
    }
}
