package com.gamss.android.domain.conversation

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.usecase.UseCase
import javax.inject.Inject

class GetConversationUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
) : UseCase<Long, AppResult<ConversationDetail>> {

    override suspend fun invoke(params: Long): AppResult<ConversationDetail> =
        conversationRepository.getConversation(params)
}
