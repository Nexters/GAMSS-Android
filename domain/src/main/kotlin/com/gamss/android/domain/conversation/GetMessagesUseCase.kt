package com.gamss.android.domain.conversation

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.usecase.UseCase
import javax.inject.Inject

class GetMessagesUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
) : UseCase<Long, AppResult<List<Message>>> {

    override suspend fun invoke(params: Long): AppResult<List<Message>> =
        conversationRepository.getMessages(params)
}
