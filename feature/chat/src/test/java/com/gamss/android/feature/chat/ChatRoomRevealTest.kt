package com.gamss.android.feature.chat

import com.gamss.android.domain.safety.RiskLevel
import com.gamss.android.domain.safety.RiskLexicon
import com.gamss.android.domain.safety.RiskTerm
import kotlinx.coroutines.CompletableDeferred
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
            skipItems(1) // 위험 검사 통과 후 말풍선을 그리고 입력칸을 비움

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
            skipItems(1) // 위험 검사 통과 후 말풍선을 그리고 입력칸을 비움

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
            skipItems(1) // 위험 검사 통과 후 말풍선을 그리고 입력칸을 비움

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
            awaitState() // isSending = true
            awaitState() // 위험 검사 통과 후 말풍선을 그리고 입력칸을 비움
            awaitState()
            expectSideEffect(ChatRoomSideEffect.ShowToast(SEND_FAILED_MESSAGE))

            containerHost.onSend()
            awaitState() // isSending = true
            awaitState() // 위험 검사 통과 후 말풍선을 그리고 입력칸을 비움
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
    fun 전송_버튼을_누르면_위험_검사가_끝날_때까지_입력칸을_비우지_않는다() = runTest {
        val viewModel = viewModel(commentCount = 1)

        viewModel.test(this) {
            containerHost.onInputChange(INPUT)
            skipItems(1) // input 반영
            containerHost.onSend()

            val afterSendPressed = awaitState()
            assertTrue(afterSendPressed.isSending)
            assertEquals(INPUT, afterSendPressed.input)
            assertTrue(afterSendPressed.messages.isEmpty())

            val afterRiskCheckPassed = awaitState()
            assertEquals("", afterRiskCheckPassed.input)
            assertEquals(listOf(INPUT), afterRiskCheckPassed.messages.map { it.content })

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 전송이_실패하면_말풍선을_지우고_입력칸에_보낸_내용을_복구한다() = runTest {
        val repository = FakeConversationRepository(commentCount = 1, failing = true)
        val viewModel = viewModel(repository)

        viewModel.test(this) {
            containerHost.onInputChange(INPUT)
            awaitState()
            containerHost.onSend()
            awaitState() // isSending = true

            val afterSendPressed = awaitState()
            assertEquals("", afterSendPressed.input)
            assertEquals(listOf(INPUT), afterSendPressed.messages.map { it.content })

            val afterFailure = awaitState()
            expectSideEffect(ChatRoomSideEffect.ShowToast(SEND_FAILED_MESSAGE))

            assertFalse(afterFailure.isSending)
            assertEquals(INPUT, afterFailure.input)
            assertTrue(afterFailure.messages.isEmpty())
        }
    }

    @Test
    fun 응답을_기다리는_동안_새로_입력했다면_전송이_실패해도_입력칸을_덮어쓰지_않는다() = runTest {
        val sendGate = CompletableDeferred<Unit>()
        val repository = FakeConversationRepository(commentCount = 1, failing = true, sendGate = sendGate)
        val viewModel = viewModel(repository)

        viewModel.test(this) {
            containerHost.onInputChange(INPUT)
            awaitState()
            containerHost.onSend()
            awaitState() // isSending = true
            awaitState() // 위험 검사 통과 후 말풍선을 그리고 입력칸을 비움, 응답은 sendGate에 걸려 대기 중

            containerHost.onInputChange(SECOND_INPUT)
            assertEquals(SECOND_INPUT, awaitState().input)

            sendGate.complete(Unit)

            val afterFailure = awaitState()
            expectSideEffect(ChatRoomSideEffect.ShowToast(SEND_FAILED_MESSAGE))

            assertFalse(afterFailure.isSending)
            assertEquals(SECOND_INPUT, afterFailure.input)
            assertTrue(afterFailure.messages.isEmpty())
        }
    }

    @Test
    fun 위험_감지가_치명적이면_말풍선을_그리지_않고_전송을_중단한다() = runTest {
        val riskInput = "죽고싶다"
        val viewModel = chatRoomViewModel(
            conversationRepository = FakeConversationRepository(commentCount = 1),
            riskLexiconRepository = FixedRiskLexiconRepository(
                RiskLexicon(
                    version = 1,
                    terms = listOf(RiskTerm(term = riskInput, level = RiskLevel.CRITICAL)),
                    safePhrases = emptyList(),
                    agencies = emptyList(),
                ),
            ),
        )

        viewModel.test(this) {
            containerHost.onInputChange(riskInput)
            awaitState()
            containerHost.onSend()

            val afterSendPressed = awaitState()
            assertTrue(afterSendPressed.isSending)
            assertEquals(riskInput, afterSendPressed.input)
            assertTrue(afterSendPressed.messages.isEmpty())

            val afterBlock = awaitState()
            assertFalse(afterBlock.isSending)
            assertEquals(riskInput, afterBlock.input)
            assertTrue(afterBlock.messages.isEmpty())
            assertEquals(RiskLevel.CRITICAL, afterBlock.riskDetection?.level)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 전송이_성공하면_서버_메시지의_listKey가_임시_말풍선의_id와_같다() = runTest {
        val repository = FakeConversationRepository(commentCount = 1)
        val viewModel = viewModel(repository)

        viewModel.test(this) {
            containerHost.onInputChange(INPUT)
            awaitState()
            containerHost.onSend()
            awaitState() // isSending = true

            val afterRiskCheckPassed = awaitState()
            val localId = afterRiskCheckPassed.messages.single().id

            val afterSuccess = awaitState()
            val serverMessage = afterSuccess.messages.single()
            assertEquals(localId, afterSuccess.listKeyOf(serverMessage))

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 위험_검사가_끝나기_전에_새로_입력하면_검사_통과_후에도_입력칸을_비우지_않는다() = runTest {
        val gate = CompletableDeferred<Unit>()
        val viewModel = chatRoomViewModel(
            conversationRepository = FakeConversationRepository(commentCount = 1),
            riskLexiconRepository = GatedRiskLexiconRepository(
                lexicon = RiskLexicon(
                    version = 0,
                    terms = emptyList(),
                    safePhrases = emptyList(),
                    agencies = emptyList(),
                ),
                gate = gate,
            ),
        )

        viewModel.test(this) {
            containerHost.onInputChange(INPUT)
            awaitState()
            containerHost.onSend()
            awaitState() // isSending = true, 위험 검사가 gate에 걸려 대기 중

            containerHost.onInputChange(SECOND_INPUT)
            val afterRetype = awaitState()
            assertEquals(SECOND_INPUT, afterRetype.input)

            gate.complete(Unit)

            val afterRiskCheckPassed = awaitState()
            assertEquals(SECOND_INPUT, afterRiskCheckPassed.input)
            assertEquals(listOf(INPUT), afterRiskCheckPassed.messages.map { it.content })

            cancelAndIgnoreRemainingItems()
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
            awaitState() // isSending = true
            awaitState() // 위험 검사 통과 후 말풍선을 그리고 입력칸을 비움
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
