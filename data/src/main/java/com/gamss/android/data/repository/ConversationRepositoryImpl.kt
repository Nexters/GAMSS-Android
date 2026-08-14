package com.gamss.android.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.gamss.android.core.common.AppResult
import com.gamss.android.core.common.map
import com.gamss.android.data.chattingsearch.ChattingRoomSearchPagingSource
import com.gamss.android.data.di.ApplicationScope
import com.gamss.android.data.remote.conversation.ConversationService
import com.gamss.android.data.remote.conversation.model.request.SaveMessageRequest
import com.gamss.android.data.remote.conversation.model.request.UpdateConversationTitleRequest
import com.gamss.android.data.remote.conversation.model.response.ConversationMessage
import com.gamss.android.data.remote.conversation.model.response.ConversationResponse
import com.gamss.android.data.remote.conversation.model.response.toDomain
import com.gamss.android.data.remote.emotion.toServerEmotionType
import com.gamss.android.data.remote.runCatchingApiCall
import com.gamss.android.data.remote.throwIfFailed
import com.gamss.android.domain.conversation.Conversation
import com.gamss.android.domain.conversation.ConversationRepository
import com.gamss.android.domain.conversation.Message
import com.gamss.android.domain.conversation.SentMessage
import com.gamss.android.domain.conversation.chattingsearch.ChattingRoomSummary
import com.gamss.android.domain.emotion.EmotionCharacter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ConversationRepositoryImpl @Inject constructor(
    private val conversationService: ConversationService,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : ConversationRepository {

    override suspend fun getOngoingConversations(): AppResult<List<Conversation>> = runCatchingApiCall {
        val response = conversationService.getIncompleteConversations()
        checkNotNull(response.data) { "No available conversation data" }
            .mapNotNull(ConversationResponse::toDomain)
    }

    override suspend fun sendMessage(
        conversationId: Long?,
        content: String,
        replyToMessageId: Long?,
        contextSummary: String?,
        excludeCharacters: Set<EmotionCharacter>,
    ): AppResult<SentMessage> = runCatchingApiCall {
        val response = conversationService.saveMessage(
            SaveMessageRequest(
                content = content,
                conversationId = conversationId,
                repliesToMessageId = replyToMessageId,
                currentConversationSummary = contextSummary,
                excludeCharacters = excludeCharacters
                    .takeIf { it.isNotEmpty() }
                    ?.map(EmotionCharacter::toServerEmotionType),
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

    override suspend fun endConversation(conversationId: Long): AppResult<Unit> {
        val result = runCatchingApiCall {
            conversationService.endConversation(conversationId).throwIfFailed()
        }
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

    override suspend fun deleteConversation(conversationId: Long): AppResult<Unit> {
        val result = runCatchingApiCall {
            conversationService.deleteConversation(conversationId).throwIfFailed()
        }
        return when (result) {
            is AppResult.Success -> AppResult.Success(Unit)
            // 이미 지워진 방이면 목표는 달성된 상태다. 실패로 흘리면 재시도가 영원히 같은 오류를 받는다.
            is AppResult.Failure ->
                if (result.throwable.hasErrorCode(CONVERSATION_ALREADY_DELETED)) {
                    AppResult.Success(Unit)
                } else {
                    result
                }
        }
    }

    override fun searchChattingRooms(keyword: String): Flow<PagingData<ChattingRoomSummary>> =
        Pager(
            config = PagingConfig(
                pageSize = ChattingRoomSearchPagingSource.DEFAULT_SIZE,
                initialLoadSize = ChattingRoomSearchPagingSource.DEFAULT_SIZE,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = {
                ChattingRoomSearchPagingSource(
                    conversationService = conversationService,
                    keyword = keyword,
                )
            },
        ).flow
}
