package com.gamss.android.feature.chat

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
    fun 댓글은_첫_번째부터_하나씩_지연되어_노출된다() = runTest {
        val viewModel = viewModel(commentCount = 3)

        viewModel.test(this) {
            containerHost.onInputChange(INPUT)
            skipItems(1) // input 반영

            containerHost.onSend()
            skipItems(1) // isSending = true

            val afterSend = awaitState()
            assertEquals(listOf(USER_ID), afterSend.messages.map { it.id })
            assertEquals(
                listOf(COMMENT_ID_BASE + 0, COMMENT_ID_BASE + 1, COMMENT_ID_BASE + 2),
                afterSend.pendingComments.map { it.id },
            )
            assertTrue(afterSend.isAwaitingComments)
            skipItems(1) // 전송 성공 뒤 백그라운드로 갱신되는 토큰 사용량 반영

            val firstReveal = awaitState()
            assertEquals(COMMENT_ID_BASE + 0, firstReveal.messages.last().id)
            assertEquals(2, firstReveal.pendingComments.size)

            val secondReveal = awaitState()
            assertEquals(COMMENT_ID_BASE + 1, secondReveal.messages.last().id)
            assertEquals(1, secondReveal.pendingComments.size)

            val thirdReveal = awaitState()
            assertEquals(COMMENT_ID_BASE + 2, thirdReveal.messages.last().id)
            assertTrue(thirdReveal.pendingComments.isEmpty())
            assertFalse(thirdReveal.isAwaitingComments)

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
            assertEquals(4, afterSend.pendingComments.size)
            skipItems(1) // 전송 성공 뒤 백그라운드로 갱신되는 토큰 사용량 반영

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
    fun 댓글이_하나여도_지연_후_노출된다() = runTest {
        val viewModel = viewModel(commentCount = 1)

        viewModel.test(this) {
            containerHost.onInputChange(INPUT)
            skipItems(1) // input 반영
            containerHost.onSend()
            skipItems(1) // isSending = true

            val afterSend = awaitState()
            assertEquals(1, afterSend.pendingComments.size)
            assertEquals(1, afterSend.messages.size)
            skipItems(1) // 전송 성공 뒤 백그라운드로 갱신되는 토큰 사용량 반영

            val afterReveal = awaitState()
            assertTrue(afterReveal.pendingComments.isEmpty())
            assertEquals(2, afterReveal.messages.size)

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

            // 입력칸을 되돌리지 않으므로(실패한 말풍선의 재전송 버튼으로 대신한다),
            // 두 번째 시도는 그 실패한 말풍선을 재전송하는 경로로 검증한다.
            val failedMessage = containerHost.container.stateFlow.value.messages.last()
            containerHost.onRetrySend(failedMessage)
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

    @Test
    fun 전송이_성공하면_토큰_사용량_갱신을_요청한다() = runTest {
        val notifier = RecordingTokenUsageRefreshNotifier()
        val viewModel = chatRoomViewModel(
            conversationRepository = FakeConversationRepository(commentCount = 1),
            tokenUsageRefreshNotifier = notifier,
        )

        viewModel.test(this) {
            containerHost.onInputChange(INPUT)
            skipItems(1) // input 반영
            containerHost.onSend()
            skipItems(1) // isSending = true
            awaitState() // 전송 성공 반영
            advanceUntilIdle()

            assertEquals(1, notifier.refreshCount)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 전송_버튼을_누르면_응답을_기다리지_않고_입력칸이_바로_비워진다() = runTest {
        val viewModel = viewModel(commentCount = 1)

        viewModel.test(this) {
            containerHost.onInputChange(INPUT)
            skipItems(1) // input 반영
            containerHost.onSend()

            val afterSendPressed = awaitState()
            assertTrue(afterSendPressed.isSending)
            assertEquals("", afterSendPressed.input)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 전송이_실패하면_말풍선은_남고_재전송_대상으로_표시된다() = runTest {
        val repository = FakeConversationRepository(commentCount = 1, failing = true)
        val viewModel = viewModel(repository)

        viewModel.test(this) {
            containerHost.onInputChange(INPUT)
            awaitState()
            containerHost.onSend()

            val afterSendPressed = awaitState()
            assertEquals("", afterSendPressed.input)
            assertEquals(listOf(INPUT), afterSendPressed.messages.map { it.content })

            val afterFailure = awaitState()
            expectSideEffect(ChatRoomSideEffect.ShowToast(SEND_FAILED_MESSAGE))

            assertFalse(afterFailure.isSending)
            // 입력칸을 되돌리지 않는다 — 방금 그린 말풍선의 재전송 버튼(임시 UI)으로 다시 보낸다.
            assertEquals("", afterFailure.input)
            assertEquals(listOf(INPUT), afterFailure.messages.map { it.content })
            assertEquals(afterFailure.messages.single().id, afterFailure.failedMessageIds.single())
        }
    }

    @Test
    fun 전송이_실패하면_토큰_사용량_갱신을_요청하지_않는다() = runTest {
        val notifier = RecordingTokenUsageRefreshNotifier()
        val viewModel = chatRoomViewModel(
            conversationRepository = FakeConversationRepository(commentCount = 1, failing = true),
            tokenUsageRefreshNotifier = notifier,
        )

        viewModel.test(this) {
            containerHost.onInputChange(INPUT)
            awaitState()
            containerHost.onSend()
            awaitState()
            awaitState()
            expectSideEffect(ChatRoomSideEffect.ShowToast(SEND_FAILED_MESSAGE))

            assertEquals(0, notifier.refreshCount)

            cancelAndIgnoreRemainingItems()
        }
    }

    private fun viewModel(commentCount: Int): ChatRoomViewModel =
        chatRoomViewModel(FakeConversationRepository(commentCount))

    private fun viewModel(conversationRepository: FakeConversationRepository): ChatRoomViewModel =
        chatRoomViewModel(conversationRepository)

    private companion object {
        const val SEED_INPUT = "아 진짜 짜증나 지갑 잃어버렸어"
        const val SEND_FAILED_MESSAGE = "메시지를 보내지 못했어요"
    }
}
