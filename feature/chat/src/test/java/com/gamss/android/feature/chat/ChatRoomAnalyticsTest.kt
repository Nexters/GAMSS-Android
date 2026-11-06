package com.gamss.android.feature.chat

import com.gamss.android.domain.analytics.AnalyticsEvent
import com.gamss.android.domain.analytics.AnalyticsProperty
import com.gamss.android.domain.safety.RiskLevel
import com.gamss.android.domain.safety.RiskTerm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.orbitmvi.orbit.test.test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatRoomAnalyticsTest {

    private val analyticsTracker = RecordingAnalyticsTracker()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun 보내면_전송_이벤트를_남기고_빈_입력은_남기지_않는다() = runTest {
        chatRoomViewModel(analyticsTracker = analyticsTracker).test(this) {
            containerHost.onSend()
            runCurrent()
            assertEquals(emptyList<String>(), eventNames())

            containerHost.onInputChange(INPUT)
            containerHost.onSend()
            runCurrent()
            assertEquals(listOf(AnalyticsEvent.CHAT_SEND), eventNames())

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 끝내기를_확인하면_요청_확인_카드_생성_순으로_남는다() = runTest {
        chatRoomViewModel(analyticsTracker = analyticsTracker).test(this) {
            containerHost.sendOneMessage()
            runCurrent()

            containerHost.onEndRequest()
            containerHost.onEndConfirm()
            runCurrent()

            assertEquals(
                listOf(
                    AnalyticsEvent.CHAT_SEND,
                    AnalyticsEvent.CHAT_END_REQUEST,
                    AnalyticsEvent.CHAT_END_CONFIRM,
                    AnalyticsEvent.CARD_CREATE_SUCCESS,
                ),
                eventNames(),
            )
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 확인을_취소하면_취소_이벤트를_한_번만_남긴다() = runTest {
        chatRoomViewModel(analyticsTracker = analyticsTracker).test(this) {
            containerHost.sendOneMessage()
            runCurrent()

            containerHost.onEndRequest()
            containerHost.onEndCancel()
            containerHost.onEndCancel()
            runCurrent()

            assertEquals(
                listOf(AnalyticsEvent.CHAT_SEND, AnalyticsEvent.CHAT_END_REQUEST, AnalyticsEvent.CHAT_END_CANCEL),
                eventNames(),
            )
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 끝낼_수_없는_상태에서_누르면_요청_이벤트를_남기지_않는다() = runTest {
        chatRoomViewModel(analyticsTracker = analyticsTracker).test(this) {
            containerHost.onEndRequest()
            containerHost.onEndConfirm()
            runCurrent()

            assertEquals(emptyList<String>(), eventNames())
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 카드_생성_재시도는_종료_요청이_아니라_생성_결과만_남긴다() = runTest {
        val cardRepository = CountingCardRepository(failure = IllegalStateException("card failed"))

        chatRoomViewModel(cardRepository = cardRepository, analyticsTracker = analyticsTracker).test(this) {
            containerHost.sendOneMessage()
            runCurrent()
            containerHost.endConversation()
            runCurrent()

            containerHost.onEndRequest()
            runCurrent()

            assertEquals(
                listOf(
                    AnalyticsEvent.CHAT_SEND,
                    AnalyticsEvent.CHAT_END_REQUEST,
                    AnalyticsEvent.CHAT_END_CONFIRM,
                    AnalyticsEvent.CARD_CREATE_FAIL,
                    AnalyticsEvent.CARD_CREATE_FAIL,
                ),
                eventNames(),
            )
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 접기는_단계가_넘어갈_때만_새_단계와_함께_남긴다() = runTest {
        chatRoomViewModel(analyticsTracker = analyticsTracker).test(this) {
            containerHost.sendOneMessage()
            runCurrent()
            containerHost.endConversation()
            runCurrent()

            containerHost.onCardFoldTap()
            containerHost.onCardFoldTap()
            containerHost.onCardFoldTap()
            runCurrent()

            assertEquals(
                listOf(
                    AnalyticsEvent.CARD_FOLD_STEP to mapOf<String, Any>(AnalyticsProperty.STAGE to "foldedonce"),
                    AnalyticsEvent.CARD_FOLD_STEP to mapOf<String, Any>(AnalyticsProperty.STAGE to "foldedtwice"),
                ),
                analyticsTracker.events.filter { it.first == AnalyticsEvent.CARD_FOLD_STEP },
            )
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 닫기와_다시_열기는_실제로_바뀔_때만_남긴다() = runTest {
        chatRoomViewModel(analyticsTracker = analyticsTracker).test(this) {
            containerHost.sendOneMessage()
            runCurrent()
            containerHost.onCardSetAside()
            containerHost.onCardReopen()
            runCurrent()
            containerHost.endConversation()
            runCurrent()

            containerHost.onCardSetAside()
            containerHost.onCardSetAside()
            containerHost.onCardReopen()
            containerHost.onCardReopen()
            runCurrent()

            assertEquals(
                listOf(AnalyticsEvent.CARD_SET_ASIDE, AnalyticsEvent.CARD_REOPEN),
                eventNames().filter { it == AnalyticsEvent.CARD_SET_ASIDE || it == AnalyticsEvent.CARD_REOPEN },
            )
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 위험_표현으로_막히면_전송_뒤에_차단_이벤트를_남긴다() = runTest {
        val riskInput = "죽고싶다"
        val viewModel = chatRoomViewModel(
            riskLexiconRepository = FixedRiskLexiconRepository(
                EmptyRiskLexicon.copy(terms = listOf(RiskTerm(term = riskInput, level = RiskLevel.CRITICAL))),
            ),
            analyticsTracker = analyticsTracker,
        )
        viewModel.test(this) {
            containerHost.onInputChange(riskInput)
            containerHost.onSend()
            runCurrent()

            assertEquals(listOf(AnalyticsEvent.CHAT_SEND, AnalyticsEvent.CHAT_SEND_BLOCKED), eventNames())
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 전송에_실패하면_전송_뒤에_실패_이벤트를_남긴다() = runTest {
        val viewModel = chatRoomViewModel(
            conversationRepository = FakeConversationRepository(failing = true),
            analyticsTracker = analyticsTracker,
        )
        viewModel.test(this) {
            containerHost.sendOneMessage()
            runCurrent()

            assertEquals(listOf(AnalyticsEvent.CHAT_SEND, AnalyticsEvent.CHAT_SEND_FAIL), eventNames())
            cancelAndIgnoreRemainingItems()
        }
    }

    private fun eventNames(): List<String> = analyticsTracker.events.map { it.first }

    private fun ChatRoomViewModel.sendOneMessage() {
        onInputChange(INPUT)
        onSend()
    }

    private fun ChatRoomViewModel.endConversation() {
        onEndRequest()
        onEndConfirm()
    }
}
