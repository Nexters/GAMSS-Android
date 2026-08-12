package com.gamss.android.domain.conversation

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.usecase.UseCase
import javax.inject.Inject

class DeleteConversationUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
) : UseCase<Long, AppResult<Unit>> {

    override suspend fun invoke(params: Long): AppResult<Unit> =
        conversationRepository.deleteConversation(params)
}
