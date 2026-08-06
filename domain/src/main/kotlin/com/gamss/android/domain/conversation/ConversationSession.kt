package com.gamss.android.domain.conversation

import com.gamss.android.core.common.AppResult
import javax.inject.Inject

/**
 * 압축본이 서버에 확정된 USER 발화만 미러링하도록 순서를 강제한다.
 * 호출자가 조회·전송과 압축본 갱신의 순서를 직접 맞추면 진입점이 늘 때마다 규칙이 복제된다.
 */
class ConversationSession @Inject constructor(
    private val sendMessage: SendMessageUseCase,
    private val getMessages: GetMessagesUseCase,
    private val summaryStore: ConversationSummaryStore,
) {

    suspend fun restore(conversationId: Long): AppResult<List<Message>> {
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
        }
        return result
    }

    /** 요약이 돌 수 있어 호출자가 화면을 갱신한 뒤에 부른다. */
    suspend fun compactSummary() = summaryStore.compact()
}

internal fun List<Message>.userUtterances(): List<String> =
    filter { it.sender == MessageSender.User }.map { it.content }
