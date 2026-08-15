package com.gamss.android.domain.conversation

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.card.Card
import com.gamss.android.domain.card.CardNotRetryableException
import com.gamss.android.domain.card.CreateConversationCardUseCase
import com.gamss.android.domain.emotion.ConversationEmotionAccumulator
import com.gamss.android.domain.emotion.EmotionCharacter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/** 한 대화의 전송·복원·제목·종료·카드 생성 순서를 한곳에서 지키는 역할이라 의존이 그만큼 필요하다. */
@Suppress("LongParameterList")
class ConversationSession @Inject constructor(
    private val sendMessage: SendMessageUseCase,
    private val getMessages: GetMessagesUseCase,
    private val updateConversationTitle: UpdateConversationTitleUseCase,
    private val endConversation: EndConversationUseCase,
    private val createConversationCard: CreateConversationCardUseCase,
    private val summaryStore: ConversationSummaryStore,
    private val emotionAccumulator: ConversationEmotionAccumulator,
    private val pendingReveal: PendingConversationReveal,
) {

    private val titleMutex = Mutex()

    private var pendingTitle: PendingTitle? = null

    suspend fun restore(conversationId: Long): AppResult<List<Message>> {
        clearPendingTitle()
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
        excludeCharacters: Set<EmotionCharacter> = emptySet(),
    ): AppResult<SentMessage> {
        val opensConversation = conversationId == null
        // 새 대화는 이전 대화의 문맥을 물려받지 않는다. 이 인스턴스가 홈처럼 오래 사는 화면에 물려
        // 있으면 앞 대화의 발화가 남아 새 대화 첫 요청에 남의 얘기가 실려 나간다.
        if (opensConversation) resetConversationState()
        val result = sendMessage(
            SendMessageUseCase.Params(
                conversationId = conversationId,
                content = content,
                replyToMessageId = replyToMessageId,
                contextSummary = summaryStore.currentContextSummary(),
                excludeCharacters = excludeCharacters,
            ),
        )
        if (result is AppResult.Success) {
            summaryStore.append(result.data.message.content)
            emotionAccumulator.append(result.data.message.content)
            opensConversation.takeIf { it }?.let {
                savePendingTitle(
                    PendingTitle(
                        conversationId = result.data.message.conversationId,
                        seed = result.data.message.content,
                    ),
                )
                pendingReveal.save(result.data)
            }
        }
        return result
    }

    /**
     * 홈에서 새 대화를 열며 이미 받아온 첫 교환을 채팅방이 재조회 없이 그대로 이어받게 한다.
     * 채팅방이 이 대화를 처음 여는 게 아니면(캐시가 비었거나 다른 conversationId면) null이라
     * 호출부는 [restore] 경로로 폴백한다.
     *
     * 홈의 send() 는 홈 쪽 인스턴스의 요약·감정 상태만 채워 둔다. 이 인스턴스는 그 첫 발화를
     * 한 번도 보지 못했으므로, [restore] 가 하는 것과 같은 방식으로 여기서도 시드해 둬야
     * 다음 전송의 문맥 요약과 대화 종료 시 감정 결과가 비어 있지 않다.
     */
    suspend fun consumePendingReveal(conversationId: Long): SentMessage? {
        val sent = pendingReveal.consume(conversationId) ?: return null
        val utterances = listOf(sent.message).userUtterances()
        summaryStore.restore(utterances)
        emotionAccumulator.restore(utterances)
        return sent
    }

    suspend fun finishSend() = coroutineScope {
        launch { assignPendingTitle() }
        launch {
            summaryStore.compact()
            emotionAccumulator.classifyPending()
        }
    }

    private suspend fun resetConversationState() {
        summaryStore.reset()
        emotionAccumulator.reset()
    }

    private suspend fun assignPendingTitle() {
        val pending = takePendingTitle() ?: return
        val title = conversationTitleFrom(pending.seed) ?: return
        val result = try {
            updateConversationTitle(
                UpdateConversationTitleUseCase.Params(conversationId = pending.conversationId, title = title),
            )
        } catch (e: CancellationException) {
            restorePendingTitle(pending)
            throw e
        }
        if (result is AppResult.Failure) {
            pending.nextAttempt()?.let { nextPending ->
                restorePendingTitle(nextPending)
            }
        }
    }

    private suspend fun clearPendingTitle() {
        titleMutex.withLock { pendingTitle = null }
    }

    private suspend fun savePendingTitle(pending: PendingTitle) {
        titleMutex.withLock { pendingTitle = pending }
    }

    private suspend fun takePendingTitle(): PendingTitle? =
        titleMutex.withLock { pendingTitle.also { pendingTitle = null } }

    private suspend fun restorePendingTitle(pending: PendingTitle) {
        titleMutex.withLock {
            pendingTitle = pendingTitle ?: pending
        }
    }

    private data class PendingTitle(
        val conversationId: Long,
        val seed: String,
        val attempts: Int = 0,
    ) {
        fun nextAttempt(): PendingTitle? =
            copy(attempts = attempts + 1).takeIf { it.attempts < MAX_TITLE_ATTEMPTS }
    }

    private companion object {
        const val MAX_TITLE_ATTEMPTS = 3
    }

    suspend fun end(conversationId: Long): AppResult<Unit> = endConversation(conversationId)

    /**
     * 감정/요약 온디바이스 모델 다운로드를 미리 걸어둔다(예: 채팅방 진입 시점). 두 다운로드는
     * 서로 독립적이라 동시에 건다. 실패해도 이 함수는 던지지 않는다 — 실제로 필요한 시점([compact],
     * [createCard])에 정식 경로로 다시 확인·재시도되므로 순수 최적화용 호출이다.
     */
    suspend fun prefetchOnDeviceModels() = coroutineScope {
        launch { emotionAccumulator.prefetch() }
        launch { summaryStore.prefetch() }
    }

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
