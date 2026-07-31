package com.gamss.android.domain.conversation

import com.gamss.android.core.common.AppResult
import javax.inject.Inject

/** 서버 제약. 입력 UI 도 이 값으로 길이를 제한한다. */
const val MAX_MESSAGE_LENGTH = 140

/** 새 일기·이어 쓰기·답장이 같은 엔드포인트를 쓰므로 [replyToMessageId] 유무로만 갈린다. */
class SendMessageUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
) {
    suspend operator fun invoke(
        conversationId: Long?,
        content: String,
        replyToMessageId: Long? = null,
    ): AppResult<SentMessage> {
        val trimmed = content.trim()
        val invalidContent = trimmed.findConstraintViolation()
        if (invalidContent != null) {
            return AppResult.Failure(invalidContent)
        }
        return conversationRepository.sendMessage(
            conversationId = conversationId,
            content = trimmed,
            replyToMessageId = replyToMessageId,
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
