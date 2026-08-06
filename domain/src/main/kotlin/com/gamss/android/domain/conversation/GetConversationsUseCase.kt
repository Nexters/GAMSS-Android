package com.gamss.android.domain.conversation

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.usecase.UseCase
import java.time.LocalDate
import javax.inject.Inject

class GetConversationsUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
) : UseCase<LocalDate, AppResult<List<Conversation>>> {

    override suspend fun invoke(params: LocalDate): AppResult<List<Conversation>> =
        conversationRepository.getConversations(params)
}
