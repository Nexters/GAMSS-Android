package com.gamss.android.domain.conversation

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.card.Card
import com.gamss.android.domain.card.CardNotRetryableException
import com.gamss.android.domain.card.CreateConversationCardUseCase
import com.gamss.android.domain.emotion.ConversationEmotionAccumulator
import javax.inject.Inject

/**
 * 압축본과 감정 누적이 서버에 확정된 USER 발화만 미러링하도록 순서를 강제한다.
 * 호출자가 조회·전송과 파생 상태 갱신의 순서를 직접 맞추면 진입점이 늘 때마다 규칙이 복제된다.
 */
class ConversationSession @Inject constructor(
    private val sendMessage: SendMessageUseCase,
    private val getMessages: GetMessagesUseCase,
    private val endConversation: EndConversationUseCase,
    private val createConversationCard: CreateConversationCardUseCase,
    private val summaryStore: ConversationSummaryStore,
    private val emotionAccumulator: ConversationEmotionAccumulator,
) {

    suspend fun restore(conversationId: Long): AppResult<List<Message>> {
        val result = getMessages(conversationId)
        when (result) {
            is AppResult.Success -> {
                val utterances = result.data.userUtterances()
                summaryStore.restore(utterances)
                emotionAccumulator.restore(utterances)
            }
            is AppResult.Failure -> {
                summaryStore.reset()
                emotionAccumulator.reset()
            }
        }
        return result
    }

    suspend fun send(
        conversationId: Long?,
        content: String,
        replyToMessageId: Long?,
    ): AppResult<SentMessage> {
        val result = sendMessage(
            SendMessageUseCase.Params(
                conversationId = conversationId,
                content = content,
                replyToMessageId = replyToMessageId,
                contextSummary = summaryStore.currentContextSummary(),
            ),
        )
        if (result is AppResult.Success) {
            summaryStore.append(result.data.message.content)
            emotionAccumulator.append(result.data.message.content)
        }
        return result
    }

    /** 온디바이스 모델이 돌 수 있어 호출자가 화면을 갱신한 뒤에 부른다. */
    suspend fun compact() {
        summaryStore.compact()
        emotionAccumulator.classifyPending()
    }

    suspend fun end(conversationId: Long): AppResult<Unit> = endConversation(conversationId)

    /**
     * 카드는 [end] 와 묶지 않는다. 종료는 되돌릴 수 없어서, 종료는 됐고 카드만 실패한 상태를
     * 화면이 들고 있어야 한다.
     *
     * 분류가 남았는데 대표 감정이 없으면 온디바이스 모델의 일시 장애라 다시 해볼 여지가 있다.
     * 다 돌렸는데도 없으면 재시도해도 같으므로 [CardNotRetryableException] 으로 구분해 알린다.
     */
    suspend fun createCard(conversationId: Long, messages: List<Message>): AppResult<Card> {
        val classified = emotionAccumulator.classifyPending()
        val emotion = emotionAccumulator.result()
        if (emotion == null) {
            val cause = if (classified) {
                CardNotRetryableException.NoEmotion()
            } else {
                IllegalStateException("Emotion is not ready")
            }
            return AppResult.Failure(cause)
        }
        return createConversationCard(
            CreateConversationCardUseCase.Params(
                conversationId = conversationId,
                character = emotion.character,
                utterances = messages.userUtterances(),
            ),
        )
    }
}

internal fun List<Message>.userUtterances(): List<String> =
    filter { it.sender == MessageSender.User }.map { it.content }
