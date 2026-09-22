package com.gamss.android.feature.chat

import com.gamss.android.domain.card.CreateCardUseCase
import com.gamss.android.domain.card.CreateConversationCardUseCase
import com.gamss.android.domain.conversation.ConversationSession
import com.gamss.android.domain.conversation.ConversationSummaryStore
import com.gamss.android.domain.conversation.EndConversationUseCase
import com.gamss.android.domain.conversation.GetConversationUseCase
import com.gamss.android.domain.conversation.PendingConversationReveal
import com.gamss.android.domain.conversation.SendMessageUseCase
import com.gamss.android.domain.conversation.UpdateConversationTitleUseCase
import com.gamss.android.domain.emotion.ConversationEmotionAccumulator
import com.gamss.android.domain.safety.RiskLevel
import com.gamss.android.domain.safety.RiskTerm
import com.gamss.android.domain.summary.SummarizeDiaryUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.orbitmvi.orbit.test.test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatRoomLoadTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun 홈에서_보낸_첫_교환은_채팅방_진입시_순차_노출된다() = runTest {
        val repository = FakeConversationRepository(commentCount = 3)
        val reveal = PendingConversationReveal()
        val homeSession = homeSession(repository, reveal)
        homeSession.send(conversationId = null, content = INPUT, replyToMessageId = null)
        val viewModel = chatRoomViewModel(conversationRepository = repository, pendingReveal = reveal)

        viewModel.test(this) {
            containerHost.start(ROOM_ID)
            skipItems(1) // isLoading = true

            val afterLoad = awaitState()
            assertEquals(listOf(USER_ID), afterLoad.messages.map { it.id })
            assertEquals(
                listOf(COMMENT_ID_BASE + 0, COMMENT_ID_BASE + 1, COMMENT_ID_BASE + 2),
                afterLoad.pendingComments.map { it.id },
            )
            // reveal 캐시 경로도 서버 재조회 없이 top bar 제목(생성 일시)을 즉시 채운다.
            assertTrue(afterLoad.conversationCreatedAt != null)

            val firstReveal = awaitState()
            assertEquals(COMMENT_ID_BASE + 0, firstReveal.messages.last().id)

            val secondReveal = awaitState()
            assertEquals(COMMENT_ID_BASE + 1, secondReveal.messages.last().id)

            val thirdReveal = awaitState()
            assertEquals(COMMENT_ID_BASE + 2, thirdReveal.messages.last().id)
            assertTrue(thirdReveal.pendingComments.isEmpty())

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 홈에서_보낸_첫_메시지에_위험_신호가_있으면_진입시_안내를_띄운다() = runTest {
        val repository = FakeConversationRepository(commentCount = 1)
        val reveal = PendingConversationReveal()
        homeSession(repository, reveal).send(conversationId = null, content = "요즘 너무 힘들어서 우울해", replyToMessageId = null)
        val viewModel = chatRoomViewModel(
            conversationRepository = repository,
            pendingReveal = reveal,
            riskLexicon = EmptyRiskLexicon.copy(terms = listOf(RiskTerm("우울", RiskLevel.WARNING))),
        )

        viewModel.test(this) {
            containerHost.start(ROOM_ID)
            skipItems(1) // isLoading = true

            val afterLoad = awaitState()
            assertEquals(RiskLevel.WARNING, afterLoad.riskDetection?.level)
            // 이미 전송된 메시지라 막지 않는다. 그대로 노출된다.
            assertEquals(listOf(USER_ID), afterLoad.messages.map { it.id })

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun 홈에서_보낸_첫_메시지에_위험_신호가_없으면_안내를_띄우지_않는다() = runTest {
        val repository = FakeConversationRepository(commentCount = 1)
        val reveal = PendingConversationReveal()
        homeSession(repository, reveal).send(conversationId = null, content = INPUT, replyToMessageId = null)
        val viewModel = chatRoomViewModel(
            conversationRepository = repository,
            pendingReveal = reveal,
            riskLexicon = EmptyRiskLexicon.copy(terms = listOf(RiskTerm("우울", RiskLevel.WARNING))),
        )

        viewModel.test(this) {
            containerHost.start(ROOM_ID)
            skipItems(1) // isLoading = true

            assertEquals(null, awaitState().riskDetection)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun reveal_캐시가_없으면_서버에서_다시_불러온다() = runTest {
        val repository = FakeConversationRepository(commentCount = 3)
        val viewModel = chatRoomViewModel(conversationRepository = repository)

        viewModel.test(this) {
            containerHost.start(ROOM_ID)
            skipItems(1) // isLoading = true

            val afterLoad = awaitState()
            assertTrue(afterLoad.messages.isEmpty())
            assertTrue(afterLoad.pendingComments.isEmpty())

            cancelAndIgnoreRemainingItems()
        }
    }

    /** 홈 화면이 실제로 받는 것과 같은 모양의, 채팅방과는 별개인 ConversationSession 인스턴스. */
    private fun homeSession(repository: FakeConversationRepository, pendingReveal: PendingConversationReveal) =
        ConversationSession(
            sendMessage = SendMessageUseCase(repository),
            getConversation = GetConversationUseCase(repository),
            updateConversationTitle = UpdateConversationTitleUseCase(repository),
            endConversation = EndConversationUseCase(repository),
            createConversationCard = CreateConversationCardUseCase(
                summarizeDiary = SummarizeDiaryUseCase(PassThroughSummarizer),
                createCard = CreateCardUseCase(CountingCardRepository()),
            ),
            summaryStore = ConversationSummaryStore(
                summarizer = PassThroughSummarizer,
                tokenCounter = CharLengthTokenCounter,
            ),
            emotionAccumulator = ConversationEmotionAccumulator(FlatClassifier),
            pendingReveal = pendingReveal,
        )
}
