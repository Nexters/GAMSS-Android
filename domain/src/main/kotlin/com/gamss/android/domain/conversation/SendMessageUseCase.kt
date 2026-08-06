package com.gamss.android.domain.conversation

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.usecase.UseCase
import javax.inject.Inject

const val MAX_MESSAGE_LENGTH = 140

class SendMessageUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
) : UseCase<SendMessageUseCase.Params, AppResult<SentMessage>> {

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
            contextSummary = params.contextSummary?.take(MAX_CONTEXT_SUMMARY_LENGTH),
        )
    }

    private fun String.findConstraintViolation(): IllegalArgumentException? = when {
        isEmpty() -> IllegalArgumentException("Message content is blank")
        length > MAX_MESSAGE_LENGTH ->
            IllegalArgumentException("Message content exceeds $MAX_MESSAGE_LENGTH characters")
        else -> null
    }
}
