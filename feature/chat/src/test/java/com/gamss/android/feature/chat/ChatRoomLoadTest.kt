package com.gamss.android.feature.chat

import com.gamss.android.domain.card.CreateCardUseCase
import com.gamss.android.domain.card.CreateConversationCardUseCase
import com.gamss.android.domain.conversation.ConversationSession
import com.gamss.android.domain.conversation.ConversationSummaryStore
import com.gamss.android.domain.conversation.EndConversationUseCase
import com.gamss.android.domain.conversation.GetConversationUseCase
import com.gamss.android.domain.conversation.MessageSender
import com.gamss.android.domain.conversation.PendingConversationReveal
import com.gamss.android.domain.conversation.SendMessageUseCase
import com.gamss.android.domain.conversation.UpdateConversationTitleUseCase
import com.gamss.android.domain.emotion.ConversationEmotionAccumulator
import com.gamss.android.domain.emotion.EmotionCharacter
import com.gamss.android.domain.safety.RiskLevel
import com.gamss.android.domain.safety.RiskTerm
import com.gamss.android.domain.summary.SummarizeDiaryUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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

    // 경고 안내는 홈이 전송 뒤 이동 전에 띄운다. 대화방이 다시 띄우면 같은 안내를 두 번 보게 된다.
    @Test
    fun 홈에서_보낸_첫_메시지는_위험_신호가_있어도_대화방에서_다시_안내하지_않는다() = runTest {
        val repository = FakeConversationRepository(commentCount = 1)
        val reveal = PendingConversationReveal()
        homeSession(repository, reveal).send(conversationId = null, content = WARNING_INPUT, replyToMessageId = null)
        val viewModel = chatRoomViewModel(
            conversationRepository = repository,
            pendingReveal = reveal,
            riskLexicon = EmptyRiskLexicon.copy(terms = listOf(RiskTerm(WARNING_TERM, RiskLevel.WARNING))),
        )

        viewModel.test(this) {
            containerHost.start(ROOM_ID)
            skipItems(1) // isLoading = true

            val afterLoad = awaitState()
            assertEquals(null, afterLoad.riskDetection)
            assertEquals(listOf(USER_ID), afterLoad.messages.map { it.id })

            cancelAndIgnoreRemainingItems()
        }
    }

    // 첫 진입이 pending 을 소비하고 나면 재진입은 restore 경로를 탄다. 이 경로가 위험 검사를 하면
    // 이미 홈에서 본 안내가 대화방에 들어갈 때마다 다시 뜬다.
    @Test
    fun 홈에서_보낸_위험_메시지가_있는_대화에_다시_들어와도_안내를_띄우지_않는다() = runTest {
        val sent = message(id = USER_ID, conversationId = ROOM_ID, sender = MessageSender.User, content = WARNING_INPUT)
        // 서버에는 첫 진입 뒤 달린 댓글까지 있다. pending 경로라면 이 댓글은 보일 수 없다.
        val serverComment = message(
            id = COMMENT_ID_BASE,
            conversationId = ROOM_ID,
            sender = MessageSender.Character(EmotionCharacter.ANGER),
            content = "댓글",
        )
        val repository = FakeConversationRepository(restoredMessages = listOf(sent, serverComment))
        val reveal = PendingConversationReveal()
        val warningLexicon = EmptyRiskLexicon.copy(terms = listOf(RiskTerm(WARNING_TERM, RiskLevel.WARNING)))
        homeSession(repository, reveal).send(conversationId = null, content = WARNING_INPUT, replyToMessageId = null)

        val firstVisit = chatRoomViewModel(
            conversationRepository = repository,
            pendingReveal = reveal,
            riskLexicon = warningLexicon,
        )
        firstVisit.start(ROOM_ID)
        advanceUntilIdle()
        // 첫 진입은 pending 경로로 들어와 방금 보낸 메시지만 보인다.
        assertEquals(listOf(USER_ID), firstVisit.container.stateFlow.value.messages.map { it.id })

        chatRoomViewModel(conversationRepository = repository, pendingReveal = reveal, riskLexicon = warningLexicon)
            .test(this) {
                containerHost.start(ROOM_ID)
                skipItems(1) // isLoading = true

                val afterRestore = awaitState()
                assertEquals(listOf(USER_ID, COMMENT_ID_BASE), afterRestore.messages.map { it.id })
                assertEquals(null, afterRestore.riskDetection)

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

    private companion object {
        const val WARNING_TERM = "우울"
        const val WARNING_INPUT = "요즘 너무 힘들어서 우울해"
    }
}
