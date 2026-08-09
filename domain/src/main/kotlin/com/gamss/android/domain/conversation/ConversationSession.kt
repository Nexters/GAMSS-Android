package com.gamss.android.domain.conversation

import com.gamss.android.core.common.AppResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

class ConversationSession @Inject constructor(
    private val sendMessage: SendMessageUseCase,
    private val getMessages: GetMessagesUseCase,
    private val updateConversationTitle: UpdateConversationTitleUseCase,
    private val summaryStore: ConversationSummaryStore,
) {

    private val titleMutex = Mutex()

    private var pendingTitle: PendingTitle? = null

    suspend fun restore(conversationId: Long): AppResult<List<Message>> {
        titleMutex.withLock { pendingTitle = null }
        val result = getMessages(conversationId)
        when (result) {
            is AppResult.Success -> summaryStore.restore(result.data.userUtterances())
            is AppResult.Failure -> summaryStore.reset()
        }
        return result
    }

    suspend fun send(
        conversationId: Long?,
        content: String,
        replyToMessageId: Long?,
    ): AppResult<SentMessage> {
        val opensConversation = conversationId == null
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
            if (opensConversation) {
                titleMutex.withLock {
                    pendingTitle = PendingTitle(
                        conversationId = result.data.message.conversationId,
                        seed = result.data.message.content,
                    )
                }
            }
        }
        return result
    }

    suspend fun finishSend() = coroutineScope {
        launch { assignPendingTitle() }
        launch { summaryStore.compact() }
        Unit
    }

    private suspend fun assignPendingTitle() {
        val pending = titleMutex.withLock { pendingTitle.also { pendingTitle = null } } ?: return
        val title = conversationTitleFrom(pending.seed)
            ?: return
        val result = try {
            updateConversationTitle(
                UpdateConversationTitleUseCase.Params(conversationId = pending.conversationId, title = title),
            )
        } catch (e: CancellationException) {
            titleMutex.withLock {
                if (pendingTitle == null) pendingTitle = pending
            }
            throw e
        }
        if (result is AppResult.Failure && pending.attempts + 1 < MAX_TITLE_ATTEMPTS) {
            titleMutex.withLock {
                if (pendingTitle == null) pendingTitle = pending.copy(attempts = pending.attempts + 1)
            }
        }
    }

    private data class PendingTitle(
        val conversationId: Long,
        val seed: String,
        val attempts: Int = 0,
    )

    private companion object {
        const val MAX_TITLE_ATTEMPTS = 3
    }
}

internal fun List<Message>.userUtterances(): List<String> =
    filter { it.sender == MessageSender.User }.map { it.content }
