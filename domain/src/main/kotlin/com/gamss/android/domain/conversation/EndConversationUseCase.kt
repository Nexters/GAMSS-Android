package com.gamss.android.domain.conversation

import com.gamss.android.core.common.AppResult
import javax.inject.Inject

class EndConversationUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
) {
    suspend operator fun invoke(conversationId: Long): AppResult<Unit> =
        conversationRepository.endConversation(conversationId)
}
