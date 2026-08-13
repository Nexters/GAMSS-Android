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
            }
        }
        return result
    }

    suspend fun finishSend() = coroutineScope {
        launch { assignPendingTitle() }
        launch {
            summaryStore.compact()
            emotionAccumulator.classifyPending()
        }
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
