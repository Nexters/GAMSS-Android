package com.gamss.android.domain.conversation

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.usecase.UseCase
import javax.inject.Inject

const val SERVER_TITLE_LENGTH_LIMIT = 100

class UpdateConversationTitleUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
) : UseCase<UpdateConversationTitleUseCase.Params, AppResult<Unit>> {

    data class Params(
        val conversationId: Long,
        val title: String,
    )

    override suspend fun invoke(params: Params): AppResult<Unit> {
        val title = params.title.trim().take(SERVER_TITLE_LENGTH_LIMIT)
        if (title.isEmpty()) {
            return AppResult.Failure(IllegalArgumentException("Conversation title is blank"))
        }
        return conversationRepository.updateTitle(
            conversationId = params.conversationId,
            title = title,
        )
    }
}
