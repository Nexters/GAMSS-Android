package com.gamss.android.data.remote.conversation

import com.gamss.android.data.remote.conversation.model.request.SaveMessageRequest
import com.gamss.android.data.remote.conversation.model.request.UpdateConversationTitleRequest
import com.gamss.android.data.remote.conversation.model.response.ConversationMessage
import com.gamss.android.data.remote.conversation.model.response.ConversationResponse
import com.gamss.android.data.remote.conversation.model.response.SaveMessageResponse
import com.gamss.android.data.remote.model.response.ApiResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

internal interface ConversationService {

    @GET("/api/conversations/incomplete")
    suspend fun getIncompleteConversations(): ApiResponse<List<ConversationResponse>>

    @POST("/api/conversations/messages")
    suspend fun saveMessage(@Body request: SaveMessageRequest): ApiResponse<SaveMessageResponse>

    @GET("/api/conversations/{conversationId}/messages")
    suspend fun getMessages(@Path("conversationId") conversationId: Long): ApiResponse<List<ConversationMessage>>

    @PATCH("/api/conversations/{conversationId}/title")
    suspend fun updateTitle(
        @Path("conversationId") conversationId: Long,
        @Body request: UpdateConversationTitleRequest,
    ): ApiResponse<ConversationResponse>

    @POST("/api/conversations/{conversationId}/end")
    suspend fun endConversation(
        @Path("conversationId") conversationId: Long,
    ): ApiResponse<Unit>
}
