package com.gamss.android.domain.conversation

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.emotion.EmotionCharacter
import com.gamss.android.domain.usecase.UseCase
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
) : UseCase<SendMessageUseCase.Params, AppResult<SentMessage>> {

    data class Params(
        val conversationId: Long?,
        val content: String,
        val replyToMessageId: Long? = null,
        val contextSummary: String? = null,
        val excludeCharacters: Set<EmotionCharacter> = emptySet(),
    )

    override suspend fun invoke(params: Params): AppResult<SentMessage> {
        val trimmed = params.content.trim()
        val excluded = params.excludeCharacters.forNewConversation(params.conversationId)
        val violation = trimmed.findConstraintViolation() ?: excluded.findConstraintViolation()
        if (violation != null) {
            return AppResult.Failure(violation)
        }
        return conversationRepository.sendMessage(
            conversationId = params.conversationId,
            content = trimmed,
            replyToMessageId = params.replyToMessageId,
            contextSummary = params.contextSummary?.take(MAX_CONTEXT_SUMMARY_LENGTH),
            excludeCharacters = excluded,
        )
    }

    private fun String.findConstraintViolation(): IllegalArgumentException? = when {
        isEmpty() -> IllegalArgumentException("Message content is blank")
        length > MAX_MESSAGE_LENGTH ->
            IllegalArgumentException("Message content exceeds $MAX_MESSAGE_LENGTH characters")
        else -> null
    }

    /** 이어 보내는 요청에서는 서버가 무시하는 값이라 애초에 싣지 않는다. */
    private fun Set<EmotionCharacter>.forNewConversation(conversationId: Long?): Set<EmotionCharacter> =
        if (conversationId == null) this else emptySet()

    private fun Set<EmotionCharacter>.findConstraintViolation(): IllegalArgumentException? =
        if (size > MAX_EXCLUDE_CHARACTERS) {
            IllegalArgumentException("At most $MAX_EXCLUDE_CHARACTERS characters can be excluded")
        } else {
            null
        }

    private companion object {
        /** 서버 규칙. 전체 제외는 불가라 한 명은 반드시 반응한다. */
        const val MAX_EXCLUDE_CHARACTERS = 5
    }
}
