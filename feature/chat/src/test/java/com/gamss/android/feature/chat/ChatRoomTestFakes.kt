package com.gamss.android.feature.chat

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.card.Card
import com.gamss.android.domain.card.CardRepository
import com.gamss.android.domain.card.CreateCardUseCase
import com.gamss.android.domain.card.CreateConversationCardUseCase
import com.gamss.android.domain.conversation.CommentGenerationStatus
import com.gamss.android.domain.conversation.ConversationRepository
import com.gamss.android.domain.conversation.ConversationSession
import com.gamss.android.domain.conversation.ConversationSummaryStore
import com.gamss.android.domain.conversation.EndConversationUseCase
import com.gamss.android.domain.conversation.GetMessagesUseCase
import com.gamss.android.domain.conversation.Message
import com.gamss.android.domain.conversation.MessageSender
import com.gamss.android.domain.conversation.SendMessageUseCase
import com.gamss.android.domain.conversation.SentMessage
import com.gamss.android.domain.emotion.ClassificationResult
import com.gamss.android.domain.emotion.ConversationEmotionAccumulator
import com.gamss.android.domain.emotion.EmotionCharacter
import com.gamss.android.domain.emotion.EmotionClassifier
import com.gamss.android.domain.emotion.EmotionLabel
import com.gamss.android.domain.repository.TokenUsageRefreshNotifier
import com.gamss.android.domain.summary.DiarySummarizer
import com.gamss.android.domain.summary.SummarizeDiaryUseCase
import com.gamss.android.domain.summary.UtteranceTokenCounter
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * 채팅 ViewModel 조립을 한 곳에 둔다. 테스트마다 따로 조립하면 세션 구성이 갈라진다.
 *
 * [summarizer] 는 카드 요약에만 쓴다. 압축본은 원문이 그대로 남아야 검증할 수 있다.
 */
internal fun chatRoomViewModel(
    conversationRepository: ConversationRepository = FakeConversationRepository(),
    cardRepository: CardRepository = CountingCardRepository(),
    summarizer: DiarySummarizer = PassThroughSummarizer,
    classifier: EmotionClassifier = FlatClassifier,
    tokenUsageRefreshNotifier: TokenUsageRefreshNotifier = RecordingTokenUsageRefreshNotifier(),
): ChatRoomViewModel = ChatRoomViewModel(
    tokenUsageRefreshNotifier = tokenUsageRefreshNotifier,
    session = ConversationSession(
        sendMessage = SendMessageUseCase(conversationRepository),
        getMessages = GetMessagesUseCase(conversationRepository),
        endConversation = EndConversationUseCase(conversationRepository),
        createConversationCard = CreateConversationCardUseCase(
            summarizeDiary = SummarizeDiaryUseCase(summarizer),
            createCard = CreateCardUseCase(cardRepository),
        ),
        summaryStore = ConversationSummaryStore(
            summarizer = PassThroughSummarizer,
            tokenCounter = CharLengthTokenCounter,
        ),
        emotionAccumulator = ConversationEmotionAccumulator(classifier),
    ),
)

/** 갱신 요청 횟수만 센다. 홈 쪽 수신은 feature:home 테스트가 본다. */
internal class RecordingTokenUsageRefreshNotifier : TokenUsageRefreshNotifier {
    var refreshCount = 0
        private set

    override val refreshEvents: Flow<Unit> = emptyFlow()

    override fun requestRefresh() {
        refreshCount++
    }
}

internal object PassThroughSummarizer : DiarySummarizer {
    override suspend fun summarize(text: String): String = text
}

internal object BlankSummarizer : DiarySummarizer {
    override suspend fun summarize(text: String): String = ""
}

internal object CharLengthTokenCounter : UtteranceTokenCounter {
    override suspend fun count(text: String): Int = text.length
}

internal object FlatClassifier : EmotionClassifier {
    override suspend fun classify(text: String): ClassificationResult {
        val scores = EmotionLabel.entries.associate { it.koLabel to 0.1f }
        return ClassificationResult(EmotionLabel.ANGER.koLabel, 0.1f, scores)
    }
}

internal class FakeConversationRepository(
    private val commentCount: Int = 0,
    private val failing: Boolean = false,
    private val endFailing: Boolean = false,
) : ConversationRepository {
    private var sentCount = 0

    val sentContextSummaries = mutableListOf<String?>()

    override suspend fun sendMessage(
        conversationId: Long?,
        content: String,
        replyToMessageId: Long?,
        contextSummary: String?,
    ): AppResult<SentMessage> {
        sentContextSummaries += contextSummary
        if (failing) return AppResult.Failure(IllegalStateException("send failed"))
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

    override suspend fun endConversation(conversationId: Long): AppResult<Unit> =
        if (endFailing) AppResult.Failure(IllegalStateException("end failed")) else AppResult.Success(Unit)
}

/** 호출 횟수를 세고, [gate] 가 있으면 그때까지 응답을 붙든다. */
internal class CountingCardRepository(
    private val gate: CompletableDeferred<Unit>? = null,
    private val failure: Throwable? = null,
) : CardRepository {
    var calls = 0
        private set

    override suspend fun createCard(
        conversationId: Long,
        character: EmotionCharacter,
        summary: String,
    ): AppResult<Card> {
        calls++
        gate?.await()
        return failure?.let { AppResult.Failure(it) }
            ?: AppResult.Success(Card(character = character, summary = summary, message = "대사"))
    }
}

internal fun message(id: Long, conversationId: Long, sender: MessageSender, content: String) = Message(
    id = id,
    conversationId = conversationId,
    sender = sender,
    content = content,
)

internal const val INPUT = "오늘 억울한 일이 있었어"
internal const val SECOND_INPUT = "팀장이 갑자기 일을 더 줬어"

/** [SummarizeDiaryUseCase] 가 짧은 입력은 요약기를 태우지 않고 원문을 쓴다. */
internal const val LONG_INPUT = "오늘 팀장이 갑자기 일을 더 줘서 억울했고 저녁까지 남아 정리하느라 지쳤는데 아무도 몰라줘서 서운했어"
internal const val ROOM_ID = 7L
internal const val USER_ID = 100L
internal const val COMMENT_ID_BASE = 200L
internal const val COMMENT_ID_STRIDE = 10L
