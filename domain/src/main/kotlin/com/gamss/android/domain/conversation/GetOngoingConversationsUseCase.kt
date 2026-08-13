package com.gamss.android.domain.conversation

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.usecase.NoParamUseCase
import javax.inject.Inject

class GetOngoingConversationsUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
) : NoParamUseCase<AppResult<List<Conversation>>> {

    override suspend fun invoke(): AppResult<List<Conversation>> =
        conversationRepository.getOngoingConversations()
}
