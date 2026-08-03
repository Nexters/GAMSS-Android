package com.gamss.android.domain.conversation

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.usecase.UseCase
import javax.inject.Inject

/** 서버 제약. 입력 UI 도 이 값으로 길이를 제한한다. */
const val MAX_MESSAGE_LENGTH = 140

/** 새 일기·이어 쓰기·답장이 같은 엔드포인트를 쓰므로 [Params.replyToMessageId] 유무로만 갈린다. */
class SendMessageUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
) : UseCase<SendMessageUseCase.Params, AppResult<SentMessage>> {

    /** @param conversationId null 이면 서버가 새 채팅방을 만든다. */
    data class Params(
        val conversationId: Long?,
        val content: String,
        val replyToMessageId: Long? = null,
    )

    override suspend fun invoke(params: Params): AppResult<SentMessage> {
        val trimmed = params.content.trim()
        val invalidContent = trimmed.findConstraintViolation()
        if (invalidContent != null) {
            return AppResult.Failure(invalidContent)
        }
        return conversationRepository.sendMessage(
            conversationId = params.conversationId,
            content = trimmed,
            replyToMessageId = params.replyToMessageId,
        )
    }

    /** 예외 메시지에 사용자 입력 원문은 담지 않는다. */
    private fun String.findConstraintViolation(): IllegalArgumentException? = when {
        isEmpty() -> IllegalArgumentException("Message content is blank")
        length > MAX_MESSAGE_LENGTH ->
            IllegalArgumentException("Message content exceeds $MAX_MESSAGE_LENGTH characters")
        else -> null
    }
}
