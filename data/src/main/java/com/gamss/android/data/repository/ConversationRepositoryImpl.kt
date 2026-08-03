package com.gamss.android.data.repository

import com.gamss.android.core.common.AppResult
import com.gamss.android.data.remote.conversation.ConversationService
import com.gamss.android.data.remote.conversation.model.request.SaveMessageRequest
import com.gamss.android.data.remote.conversation.model.response.ConversationMessage
import com.gamss.android.data.remote.conversation.model.response.toDomain
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
    ): AppResult<SentMessage> = runCatchingApiCall {
        val response = conversationService.saveMessage(
            SaveMessageRequest(
                content = content,
                conversationId = conversationId,
                repliesToMessageId = replyToMessageId,
            ),
        )
        checkNotNull(response.data) { "No available saved message data" }.toDomain()
    }

    override suspend fun getMessages(conversationId: Long): AppResult<List<Message>> = runCatchingApiCall {
        val response = conversationService.getMessages(conversationId)
        checkNotNull(response.data) { "No available message data" }
            .mapNotNull(ConversationMessage::toDomain)
    }
}
