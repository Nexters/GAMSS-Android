package com.gamss.android.domain.conversation

import com.gamss.android.core.common.AppResult
import javax.inject.Inject

class GetMessagesUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
) {
    suspend operator fun invoke(conversationId: Long): AppResult<List<Message>> =
        conversationRepository.getMessages(conversationId)
}
