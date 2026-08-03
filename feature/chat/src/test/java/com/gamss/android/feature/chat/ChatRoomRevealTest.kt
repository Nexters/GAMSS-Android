package com.gamss.android.feature.chat

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.conversation.CommentGenerationStatus
import com.gamss.android.domain.conversation.ConversationRepository
import com.gamss.android.domain.conversation.ConversationSummaryStore
import com.gamss.android.domain.conversation.GetMessagesUseCase
import com.gamss.android.domain.conversation.Message
import com.gamss.android.domain.conversation.MessageSender
import com.gamss.android.domain.conversation.SendMessageUseCase
import com.gamss.android.domain.conversation.SentMessage
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

/**
 * 캐릭터 댓글 순차 노출의 상태 기계를 가상 시간으로 고정한다.
 * 간격 자체(1~3초)는 정책 테스트가 보고, 여기서는 순서와 큐 소진을 본다.
 */
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
            expectInitialState()
            containerHost.onInputChange(INPUT)
            skipItems(1) // input 반영

            containerHost.onSend()
            skipItems(1) // isSending = true

            val afterSend = awaitState()
            assertEquals(listOf(USER_ID, COMMENT_ID_BASE + 0), afterSend.messages.map { it.id })
            assertEquals(listOf(COMMENT_ID_BASE + 1, COMMENT_ID_BASE + 2), afterSend.pendingComments.map { it.id })
            assertTrue(afterSend.isReceiving)

            val firstReveal = awaitState()
            assertEquals(COMMENT_ID_BASE + 1, firstReveal.messages.last().id)
            assertEquals(1, firstReveal.pendingComments.size)

            val secondReveal = awaitState()
            assertEquals(COMMENT_ID_BASE + 2, secondReveal.messages.last().id)
            assertTrue(secondReveal.pendingComments.isEmpty())
            assertFalse(secondReveal.isReceiving)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 노출_중_새로_보내면_남은_댓글이_한꺼번에_붙는다() = runTest {
        val viewModel = viewModel(commentCount = 4)

        viewModel.test(this) {
            expectInitialState()
            containerHost.onInputChange(INPUT)
            skipItems(1) // input 반영
            containerHost.onSend()
            skipItems(1) // isSending = true

            val afterSend = awaitState()
            assertEquals(3, afterSend.pendingComments.size)

            containerHost.onInputChange(INPUT)
            skipItems(1) // input 반영
            containerHost.onSend()

            // 노출이 취소되고 남은 3개가 한 번에 붙는다.
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
            expectInitialState()
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

    private fun viewModel(commentCount: Int): ChatRoomViewModel {
        val conversationRepository = FakeConversationRepository(commentCount)
        return ChatRoomViewModel(
            sendMessage = SendMessageUseCase(conversationRepository),
            getMessages = GetMessagesUseCase(conversationRepository),
            summaryStore = ConversationSummaryStore(
                summarizer = PassThroughSummarizer,
                tokenCounter = CharLengthTokenCounter,
            ),
        )
    }

    private object PassThroughSummarizer : DiarySummarizer {
        override suspend fun summarize(text: String): String = text
    }

    /** 글자 수를 토큰 수로 쓴다. 노출 테스트는 청크 경계에 관심이 없다. */
    private object CharLengthTokenCounter : UtteranceTokenCounter {
        override suspend fun count(text: String): Int = text.length
    }

    private class FakeConversationRepository(private val commentCount: Int) : ConversationRepository {
        private var sentCount = 0

        override suspend fun sendMessage(
            conversationId: Long?,
            content: String,
            replyToMessageId: Long?,
            contextSummary: String?,
        ): AppResult<SentMessage> {
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

        override suspend fun getMessages(conversationId: Long): AppResult<List<Message>> =
            AppResult.Success(emptyList())
    }

    private companion object {
        const val INPUT = "오늘 억울한 일이 있었어"
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
