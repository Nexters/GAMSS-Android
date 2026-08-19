package com.gamss.android.feature.chat

import com.gamss.android.domain.card.CardNotRetryableException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.orbitmvi.orbit.test.test

/** 종료와 카드 생성 단계 전이. 각 단계에서 무엇이 가능한지를 [EndFlow] 로 확인한다. */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatRoomEndFlowTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun 끝내기는_확인을_거쳐야_카드까지_간다() = runTest {
        chatRoomViewModel().test(this) {
            containerHost.sendOneMessage()
            runCurrent()

            containerHost.onEndRequest()
            runCurrent()
            assertEquals(EndFlow.Confirming, containerHost.endFlow())

            containerHost.onEndConfirm()
            runCurrent()

            val endFlow = containerHost.endFlow()
            assertTrue(endFlow is EndFlow.CardReady)
            assertEquals(INPUT, (endFlow as EndFlow.CardReady).card.summary)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 확인을_취소하면_원래대로_돌아온다() = runTest {
        chatRoomViewModel().test(this) {
            containerHost.sendOneMessage()
            runCurrent()

            containerHost.onEndRequest()
            containerHost.onEndCancel()
            runCurrent()

            assertEquals(EndFlow.NotStarted, containerHost.endFlow())
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 보낸_메시지가_없으면_끝낼_수_없다() = runTest {
        chatRoomViewModel().test(this) {
            containerHost.onEndRequest()
            runCurrent()

            assertEquals(EndFlow.NotStarted, containerHost.endFlow())
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 종료에_실패하면_다시_대화할_수_있다() = runTest {
        val viewModel = chatRoomViewModel(
            conversationRepository = FakeConversationRepository(endFailing = true),
        )

        viewModel.test(this) {
            containerHost.sendOneMessage()
            runCurrent()

            containerHost.endConversation()
            runCurrent()

            assertEquals(EndFlow.NotStarted, containerHost.endFlow())
            containerHost.onInputChange(INPUT)
            runCurrent()
            assertTrue(containerHost.container.stateFlow.value.canSend)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 끝난_뒤에는_더_보낼_수_없다() = runTest {
        chatRoomViewModel().test(this) {
            containerHost.sendOneMessage()
            runCurrent()

            containerHost.endConversation()
            runCurrent()

            containerHost.onInputChange(INPUT)
            runCurrent()
            assertFalse(containerHost.container.stateFlow.value.canSend)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 요약이_비면_재시도_경로를_열지_않는다() = runTest {
        val cardRepository = CountingCardRepository()

        chatRoomViewModel(cardRepository = cardRepository, summarizer = BlankSummarizer).test(this) {
            // 요약기를 태우려면 짧은 입력 우회 경로를 넘겨야 한다.
            containerHost.onInputChange(LONG_INPUT)
            containerHost.onSend()
            runCurrent()

            containerHost.endConversation()
            runCurrent()

            assertEquals(EndFlow.CardFailedFinal, containerHost.endFlow())
            assertEquals(0, cardRepository.calls)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 카드_생성이_실패하면_재시도_경로가_남는다() = runTest {
        val cardRepository = CountingCardRepository(failure = IllegalStateException("card failed"))

        chatRoomViewModel(cardRepository = cardRepository).test(this) {
            containerHost.sendOneMessage()
            runCurrent()

            containerHost.endConversation()
            runCurrent()
            assertEquals(EndFlow.CardFailedRetryable, containerHost.endFlow())

            containerHost.onEndRequest()
            runCurrent()
            assertEquals(2, cardRepository.calls)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 이미_카드가_있으면_재시도_경로를_열지_않는다() = runTest {
        val cardRepository = CountingCardRepository(failure = CardNotRetryableException.AlreadyExists())

        chatRoomViewModel(cardRepository = cardRepository).test(this) {
            containerHost.sendOneMessage()
            runCurrent()

            containerHost.endConversation()
            runCurrent()
            assertEquals(EndFlow.CardFailedFinal, containerHost.endFlow())

            // 재시도 경로가 닫혀 있어야 영구히 같은 오류를 반복하지 않는다.
            containerHost.onEndRequest()
            runCurrent()
            assertEquals(1, cardRepository.calls)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 카드_생성_중_다시_눌러도_한_번만_만든다() = runTest {
        val gate = CompletableDeferred<Unit>()
        val cardRepository = CountingCardRepository(gate = gate)

        chatRoomViewModel(cardRepository = cardRepository).test(this) {
            containerHost.sendOneMessage()
            runCurrent()

            containerHost.endConversation()
            runCurrent()
            assertEquals(1, cardRepository.calls)

            // 스피너로 버튼이 가려지기 전에 한 번 더 눌린 상황.
            containerHost.onEndRequest()
            runCurrent()
            assertEquals(1, cardRepository.calls)

            gate.complete(Unit)
            runCurrent()
            assertTrue(containerHost.endFlow() is EndFlow.CardReady)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 카드를_두_번_누르면_다_접히고_그_뒤로는_안_접힌다() = runTest {
        chatRoomViewModel().test(this) {
            containerHost.sendOneMessage()
            runCurrent()
            containerHost.endConversation()
            runCurrent()
            assertEquals(CardFoldStage.Unfolded, containerHost.foldStage())

            containerHost.onCardFoldTap()
            runCurrent()
            assertEquals(CardFoldStage.FoldedOnce, containerHost.foldStage())

            containerHost.onCardFoldTap()
            runCurrent()
            assertEquals(CardFoldStage.FoldedTwice, containerHost.foldStage())

            // 다 접힌 뒤로는 눌러도 그대로여야 한다. 다음 단계는 드래그로만 넘어간다.
            containerHost.onCardFoldTap()
            runCurrent()
            assertEquals(CardFoldStage.FoldedTwice, containerHost.foldStage())

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 카드가_없는_단계에서_접기를_눌러도_아무_일도_없다() = runTest {
        chatRoomViewModel().test(this) {
            containerHost.sendOneMessage()
            runCurrent()
            assertEquals(EndFlow.NotStarted, containerHost.endFlow())

            containerHost.onCardFoldTap()
            runCurrent()
            assertEquals(EndFlow.NotStarted, containerHost.endFlow())

            cancelAndIgnoreRemainingItems()
        }
    }

    private fun ChatRoomViewModel.endFlow(): EndFlow = container.stateFlow.value.endFlow

    private fun ChatRoomViewModel.foldStage(): CardFoldStage =
        (endFlow() as EndFlow.CardReady).foldStage

    private fun ChatRoomViewModel.sendOneMessage() {
        onInputChange(INPUT)
        onSend()
    }

    private fun ChatRoomViewModel.endConversation() {
        onEndRequest()
        onEndConfirm()
    }
}
