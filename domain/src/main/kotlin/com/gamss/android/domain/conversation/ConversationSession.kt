package com.gamss.android.domain.conversation

import com.gamss.android.core.common.AppResult
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/**
 * 압축본이 서버에 확정된 USER 발화만 미러링하도록 순서를 강제한다.
 * 호출자가 조회·전송과 압축본 갱신의 순서를 직접 맞추면 진입점이 늘 때마다 규칙이 복제된다.
 *
 * 화면 하나의 수명과 같이 간다. 오래 살리면(@Singleton 등) 취소로 결과를 못 받은 제목 요청이
 * pending 으로 남아 이미 성공한 지정을 다시 보낸다.
 */
class ConversationSession @Inject constructor(
    private val sendMessage: SendMessageUseCase,
    private val getMessages: GetMessagesUseCase,
    private val updateConversationTitle: UpdateConversationTitleUseCase,
    private val summaryStore: ConversationSummaryStore,
) {

    private val titleMutex = Mutex()

    /** 아직 서버에 올리지 못한 제목. 어느 방 것인지 함께 들고 있어야 다른 방에 붙지 않는다. */
    private var pendingTitle: PendingTitle? = null

    suspend fun restore(conversationId: Long): AppResult<List<Message>> {
        // 이미 있는 대화의 제목은 건드리지 않는다. 제목은 첫 발화에서만 정한다.
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

    /**
     * 제목 지정과 요약이 돌 수 있어 호출자가 화면을 갱신한 뒤에 부른다.
     * 둘은 서로를 기다릴 이유가 없다. 직렬로 두면 느린 제목 요청이 압축을 붙들어,
     * 그 사이에 보낸 다음 발화가 압축 전 컨텍스트를 싣는다.
     */
    suspend fun finishSend() = coroutineScope {
        launch { assignPendingTitle() }
        launch { summaryStore.compact() }
        Unit
    }

    /**
     * 서버는 대화방을 제목 없이 만들고, 제목은 별도 요청으로만 붙는다.
     * 지정이 실패해도 대화는 이어져야 하므로 결과를 삼키고 다음 뒷정리에서 다시 시도하되,
     * 되돌아오지 않는 실패(권한·삭제된 방)에 매 전송마다 왕복을 붙이지 않도록 횟수를 제한한다.
     */
    private suspend fun assignPendingTitle() {
        // 꺼내면서 비워야 뒷정리가 겹쳐도 같은 제목을 두 번 보내지 않는다.
        val pending = titleMutex.withLock { pendingTitle.also { pendingTitle = null } } ?: return
        val title = conversationTitleFrom(pending.seed)
            // 제목으로 쓸 글자가 없는 시드는 다시 시도해도 결과가 같다.
            ?: return
        val result = updateConversationTitle(
            UpdateConversationTitleUseCase.Params(conversationId = pending.conversationId, title = title),
        )
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
