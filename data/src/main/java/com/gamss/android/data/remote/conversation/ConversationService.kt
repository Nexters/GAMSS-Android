package com.gamss.android.data.remote.conversation

import com.gamss.android.data.remote.conversation.model.request.SaveMessageRequest
import com.gamss.android.data.remote.conversation.model.response.ConversationMessage
import com.gamss.android.data.remote.conversation.model.response.SaveMessageResponse
import com.gamss.android.data.remote.model.response.ApiResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

internal interface ConversationService {

    @POST("/api/conversations/messages")
    suspend fun saveMessage(@Body request: SaveMessageRequest): ApiResponse<SaveMessageResponse>

    @GET("/api/conversations/{conversationId}/messages")
    suspend fun getMessages(@Path("conversationId") conversationId: Long): ApiResponse<List<ConversationMessage>>
}
