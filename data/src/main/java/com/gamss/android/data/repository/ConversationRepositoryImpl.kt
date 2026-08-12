package com.gamss.android.data.repository

import com.gamss.android.core.common.AppResult
import com.gamss.android.data.remote.conversation.ConversationService
import com.gamss.android.data.remote.conversation.model.request.SaveMessageRequest
import com.gamss.android.data.remote.conversation.model.response.ConversationMessage
import com.gamss.android.data.remote.conversation.model.response.toDomain
import com.gamss.android.data.remote.runCatchingApiCall
import com.gamss.android.domain.conversation.ConversationRepository
import com.gamss.android.domain.conversation.Message
import com.gamss.android.domain.conversation.SentMessage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ConversationRepositoryImpl @Inject constructor(
    private val conversationService: ConversationService,
) : ConversationRepository {

    override suspend fun sendMessage(
        conversationId: Long?,
        content: String,
        replyToMessageId: Long?,
        contextSummary: String?,
    ): AppResult<SentMessage> = runCatchingApiCall {
        val response = conversationService.saveMessage(
            SaveMessageRequest(
                content = content,
                conversationId = conversationId,
                repliesToMessageId = replyToMessageId,
                currentConversationSummary = contextSummary,
            ),
        )
        checkNotNull(response.data) { "No available saved message data" }.toDomain()
    }

    override suspend fun getMessages(conversationId: Long): AppResult<List<Message>> = runCatchingApiCall {
        val response = conversationService.getMessages(conversationId)
        checkNotNull(response.data) { "No available message data" }
            .mapNotNull(ConversationMessage::toDomain)
    }

    override suspend fun endConversation(conversationId: Long): AppResult<Unit> {
        val result = runCatchingApiCall { conversationService.endConversation(conversationId) }
        return when (result) {
            is AppResult.Success -> AppResult.Success(Unit)
            // 이미 종료된 방이면 목표는 달성된 상태다. 실패로 흘리면 카드 생성으로 넘어갈 수 없다.
            is AppResult.Failure ->
                if (result.throwable.hasErrorCode(CONVERSATION_ALREADY_ENDED)) {
                    AppResult.Success(Unit)
                } else {
                    result
                }
        }
    }
}
