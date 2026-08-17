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
            skipItems(1) // useChatEndFeature 반영
            skipItems(1) // isLoading = true

            val afterLoad = awaitState()
            assertEquals(listOf(USER_ID), afterLoad.messages.map { it.id })
            assertEquals(
                listOf(COMMENT_ID_BASE + 0, COMMENT_ID_BASE + 1, COMMENT_ID_BASE + 2),
                afterLoad.pendingComments.map { it.id },
            )

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
    fun reveal_캐시가_없으면_서버에서_다시_불러온다() = runTest {
        val repository = FakeConversationRepository(commentCount = 3)
        val viewModel = chatRoomViewModel(conversationRepository = repository)

        viewModel.test(this) {
            containerHost.start(ROOM_ID)
            skipItems(1) // useChatEndFeature 반영
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
