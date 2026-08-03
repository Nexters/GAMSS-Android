package com.gamss.android.domain.conversation

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.usecase.UseCase
import javax.inject.Inject

/** 서버 제약. 입력 UI 도 이 값으로 길이를 제한한다. */
const val MAX_MESSAGE_LENGTH = 140

class SendMessageUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
) : UseCase<SendMessageUseCase.Params, AppResult<SentMessage>> {

    /** @param contextSummary 생성 컨텍스트로만 쓰이는 압축본. 첫 전송에는 없다. */
    data class Params(
        val conversationId: Long?,
        val content: String,
        val replyToMessageId: Long? = null,
        val contextSummary: String? = null,
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
            // 압축본 상한은 서버 제약이다. 압축이 어긋나도 전송 자체는 살린다.
            contextSummary = params.contextSummary?.take(MAX_CONTEXT_SUMMARY_LENGTH),
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
