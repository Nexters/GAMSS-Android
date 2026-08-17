package com.gamss.android.feature.home

import androidx.paging.PagingData
import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.card.Card
import com.gamss.android.domain.card.CardEntry
import com.gamss.android.domain.card.CardRepository
import com.gamss.android.domain.card.CreateCardUseCase
import com.gamss.android.domain.card.CreateConversationCardUseCase
import com.gamss.android.domain.conversation.CommentGenerationStatus
import com.gamss.android.domain.conversation.Conversation
import com.gamss.android.domain.conversation.ConversationRepository
import com.gamss.android.domain.conversation.ConversationSession
import com.gamss.android.domain.conversation.ConversationSummaryStore
import com.gamss.android.domain.conversation.EndConversationUseCase
import com.gamss.android.domain.conversation.GetMessagesUseCase
import com.gamss.android.domain.conversation.Message
import com.gamss.android.domain.conversation.MessageSender
import com.gamss.android.domain.conversation.PendingConversationReveal
import com.gamss.android.domain.conversation.SendMessageUseCase
import com.gamss.android.domain.conversation.SentMessage
import com.gamss.android.domain.conversation.UpdateConversationTitleUseCase
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
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

internal const val NEW_ROOM_ID = 42L

internal fun conversationSession(repository: ConversationRepository) = ConversationSession(
    sendMessage = SendMessageUseCase(repository),
    getMessages = GetMessagesUseCase(repository),
    updateConversationTitle = UpdateConversationTitleUseCase(repository),
    endConversation = EndConversationUseCase(repository),
    createConversationCard = CreateConversationCardUseCase(
        summarizeDiary = SummarizeDiaryUseCase(PassThroughSummarizer),
        createCard = CreateCardUseCase(NoCardRepository),
    ),
    summaryStore = ConversationSummaryStore(
        summarizer = PassThroughSummarizer,
        tokenCounter = CharLengthTokenCounter,
    ),
    emotionAccumulator = ConversationEmotionAccumulator(FlatClassifier),
    pendingReveal = PendingConversationReveal(),
)

/**
 * 홈은 새 대화를 여는 것만 한다. 카드 생성은 이 테스트 범위 밖이라 [NoCardRepository] 가 실패시킨다.
 *
 * [gate] 를 주면 응답을 그때까지 붙든다. 전송 중 상태를 실제로 만들어야 입력 잠금을 볼 수 있다.
 */
internal class RecordingConversationRepository(
    private val failing: Boolean = false,
    private val gate: CompletableDeferred<Unit>? = null,
) : ConversationRepository {
    var sentContent: String? = null
        private set
    var sentConversationId: Long? = null
        private set
    var sentExcludeCharacters: Set<EmotionCharacter> = emptySet()
        private set
    var sendCount = 0
        private set

    /** 새 대화에 남의 문맥이 실려 나가는지 보려면 호출마다 남겨야 한다. */
    val sentContextSummaries = mutableListOf<String?>()

    override suspend fun sendMessage(
        conversationId: Long?,
        content: String,
        replyToMessageId: Long?,
        contextSummary: String?,
        excludeCharacters: Set<EmotionCharacter>,
    ): AppResult<SentMessage> {
        sendCount++
        sentConversationId = conversationId
        sentContent = content
        sentExcludeCharacters = excludeCharacters
        sentContextSummaries += contextSummary
        gate?.await()
        if (failing) return AppResult.Failure(IllegalStateException("send failed"))
        return AppResult.Success(
            SentMessage(
                message = Message(
                    id = 1L,
                    conversationId = NEW_ROOM_ID,
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

    override suspend fun updateTitle(conversationId: Long, title: String): AppResult<Unit> =
        AppResult.Success(Unit)

    override suspend fun endConversation(conversationId: Long): AppResult<Unit> =
        AppResult.Success(Unit)

    override suspend fun deleteConversation(conversationId: Long): AppResult<Unit> =
        AppResult.Success(Unit)

    override fun searchChattingRooms(keyword: String): Flow<PagingData<ChattingRoomSummary>> =
        error("홈 테스트에서 쓰지 않는다")
}

private object NoCardRepository : CardRepository {
    override suspend fun getCardsByDate(date: LocalDate): AppResult<List<Card>> =
        error("홈 테스트에서 쓰지 않는다")

    override suspend fun getCardsByMonth(yearMonth: YearMonth): AppResult<List<CardEntry>> =
        error("홈 테스트에서 쓰지 않는다")

    override suspend fun createCard(
        conversationId: Long,
        character: EmotionCharacter,
        summary: String,
    ): AppResult<Card> = error("홈 테스트에서 쓰지 않는다")
}

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
