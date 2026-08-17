package com.gamss.android.domain.conversation

import androidx.paging.PagingData
import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.card.Card
import com.gamss.android.domain.card.CardRepository
import com.gamss.android.domain.card.CreateCardUseCase
import com.gamss.android.domain.card.CreateConversationCardUseCase
import com.gamss.android.domain.conversation.chattingsearch.ChattingRoomSummary
import com.gamss.android.domain.emotion.ClassificationResult
import com.gamss.android.domain.emotion.ConversationEmotionAccumulator
import com.gamss.android.domain.emotion.EmotionCharacter
import com.gamss.android.domain.emotion.EmotionClassifier
import com.gamss.android.domain.emotion.EmotionLabel
import com.gamss.android.domain.summary.DiarySummarizer
import com.gamss.android.domain.summary.SummarizeDiaryUseCase
import com.gamss.android.domain.summary.UtteranceTokenCounter
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
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
    fun 한_세션으로_새_대화를_또_열면_앞_대화_문맥이_실리지_않는다() = runBlocking {
        val repository = FakeConversationRepository()
        val session = session(repository)

        session.openWith(SEED)
        session.openWith("주말에 약속이 겹쳐서 곤란해")

        assertEquals(listOf(null, null), repository.sentContextSummaries)
    }

    @Test
    fun 이어_쓰는_대화는_앞선_발화를_문맥으로_넘긴다() = runBlocking {
        val repository = FakeConversationRepository()
        val session = session(repository)

        session.openWith(SEED)
        session.continueWith("팀장이 또 그랬어")

        assertEquals(listOf(null, SEED), repository.sentContextSummaries)
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
    fun 제목_지정_대기_중_취소되면_다음_뒷정리에서_다시_시도한다() = runBlocking {
        val repository = FakeConversationRepository(blockingTitleUntilCancelled = true)
        val session = session(repository)

        session.send(conversationId = null, content = SEED, replyToMessageId = null)

        val finishJob = launch { session.finishSend() }
        repository.awaitTitleAttemptStart()
        finishJob.cancelAndJoin()

        assertEquals(1, repository.cancelledTitleAttempts)

        repository.blockingTitleUntilCancelled = false
        session.continueWith("팀장이 또 그랬어")

        assertEquals(listOf(ROOM_ID to TITLE, ROOM_ID to TITLE), repository.updatedTitles)
        assertEquals(listOf(ROOM_ID to TITLE), repository.committedTitles)
    }

    @Test
    fun 제목_지정이_계속_실패하면_재시도를_멈춘다() = runBlocking {
        val repository = FakeConversationRepository(failingTitle = true)
        val session = session(repository)

        session.openWith(SEED)
        repeat(5) { session.continueWith("계속 보내는 말") }

        assertEquals(3, repository.updatedTitles.size)
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
        endConversation = EndConversationUseCase(repository),
        createConversationCard = CreateConversationCardUseCase(
            summarizeDiary = SummarizeDiaryUseCase(PassThroughSummarizer),
            createCard = CreateCardUseCase(NoOpCardRepository),
        ),
        summaryStore = ConversationSummaryStore(
            summarizer = PassThroughSummarizer,
            tokenCounter = CharLengthTokenCounter,
        ),
        emotionAccumulator = ConversationEmotionAccumulator(FlatClassifier),
    )

    private object PassThroughSummarizer : DiarySummarizer {
        override suspend fun summarize(text: String): String = text
    }

    private object CharLengthTokenCounter : UtteranceTokenCounter {
        override suspend fun count(text: String): Int = text.length
    }

    private object FlatClassifier : EmotionClassifier {
        override suspend fun classify(text: String): ClassificationResult {
            val scores = EmotionLabel.entries.associate { it.koLabel to 0.1f }
            return ClassificationResult(EmotionLabel.ANGER.koLabel, 0.1f, scores)
        }
    }

    private object NoOpCardRepository : CardRepository {
        override suspend fun getCardsByDate(date: LocalDate): AppResult<List<Card>> =
            AppResult.Success(emptyList())

        override suspend fun createCard(
            conversationId: Long,
            character: EmotionCharacter,
            summary: String,
        ): AppResult<Card> = AppResult.Success(
            Card(
                id = 1L,
                conversationId = conversationId,
                character = character,
                emotionLabel = character.displayName,
                summary = summary,
                message = "대사",
                date = LocalDate.of(2026, 8, 15),
            ),
        )

        override suspend fun deleteCard(cardId: Long): AppResult<Unit> = error("사용하지 않음")
    }

    private class FakeConversationRepository(
        var failingTitle: Boolean = false,
        var blockingTitleUntilCancelled: Boolean = false,
        private val failingSend: Boolean = false,
    ) : ConversationRepository {

        val updatedTitles = mutableListOf<Pair<Long, String>>()
        val committedTitles = mutableListOf<Pair<Long, String>>()
        val sentContextSummaries = mutableListOf<String?>()
        var cancelledTitleAttempts = 0
            private set
        private var titleAttemptStarted = CompletableDeferred<Unit>()

        override suspend fun sendMessage(
            conversationId: Long?,
            content: String,
            replyToMessageId: Long?,
            contextSummary: String?,
            excludeCharacters: Set<EmotionCharacter>,
        ): AppResult<SentMessage> {
            sentContextSummaries += contextSummary
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

        override suspend fun getOngoingConversations(): AppResult<List<Conversation>> =
            AppResult.Success(emptyList())

        override suspend fun getMessages(conversationId: Long): AppResult<List<Message>> =
            AppResult.Success(emptyList())

        override suspend fun updateTitle(conversationId: Long, title: String): AppResult<Unit> {
            updatedTitles += conversationId to title
            titleAttemptStarted.complete(Unit)
            if (blockingTitleUntilCancelled) {
                try {
                    awaitCancellation()
                } finally {
                    cancelledTitleAttempts++
                }
            }
            return if (failingTitle) {
                AppResult.Failure(IllegalStateException("update title failed"))
            } else {
                committedTitles += conversationId to title
                AppResult.Success(Unit)
            }
        }

        override suspend fun endConversation(conversationId: Long): AppResult<Unit> =
            AppResult.Success(Unit)

        override suspend fun deleteConversation(conversationId: Long): AppResult<Unit> =
            AppResult.Success(Unit)

        override fun searchChattingRooms(keyword: String): Flow<PagingData<ChattingRoomSummary>> =
            throw UnsupportedOperationException()

        suspend fun awaitTitleAttemptStart() {
            titleAttemptStarted.await()
            titleAttemptStarted = CompletableDeferred()
        }
    }

    private companion object {
        const val SEED = "아 진짜 짜증나 팀장이 아이디어 가로챘어"
        const val TITLE = "팀장이 아이디어 가로챘어"
        const val ROOM_ID = 7L
        const val OTHER_ROOM_ID = 8L
        const val MESSAGE_ID = 100L
    }
}
