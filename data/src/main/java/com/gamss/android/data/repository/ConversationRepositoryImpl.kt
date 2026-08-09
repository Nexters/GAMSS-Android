package com.gamss.android.data.repository

import com.gamss.android.core.common.AppResult
import com.gamss.android.core.common.map
import com.gamss.android.data.di.ApplicationScope
import com.gamss.android.data.remote.conversation.ConversationService
import com.gamss.android.data.remote.conversation.model.request.SaveMessageRequest
import com.gamss.android.data.remote.conversation.model.request.UpdateConversationTitleRequest
import com.gamss.android.data.remote.conversation.model.response.ConversationMessage
import com.gamss.android.data.remote.conversation.model.response.ConversationResponse
import com.gamss.android.data.remote.conversation.model.response.toDomain
import com.gamss.android.domain.conversation.Conversation
import com.gamss.android.domain.conversation.ConversationRepository
import com.gamss.android.domain.conversation.Message
import com.gamss.android.domain.conversation.SentMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ConversationRepositoryImpl @Inject constructor(
    private val conversationService: ConversationService,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : ConversationRepository {

    override suspend fun getConversations(date: LocalDate): AppResult<List<Conversation>> = runCatchingApiCall {
        val response = conversationService.getConversations(date.toString())
        checkNotNull(response.data) { "No available conversation data" }
            .mapNotNull(ConversationResponse::toDomain)
    }

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

    override suspend fun updateTitle(conversationId: Long, title: String): AppResult<Unit> =
        applicationScope.async {
            runCatchingApiCall {
                conversationService.updateTitle(
                    conversationId = conversationId,
                    request = UpdateConversationTitleRequest(title = title),
                )
            }.map { }
        }.await()
}
