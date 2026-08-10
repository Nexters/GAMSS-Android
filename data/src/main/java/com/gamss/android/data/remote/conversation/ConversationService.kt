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

    /** 응답 본문은 쓰지 않는다. 서버 스키마가 바뀌어도 종료가 실패로 뒤집히지 않게 [Unit] 으로 받는다. */
    @POST("/api/conversations/{conversationId}/end")
    suspend fun endConversation(
        @Path("conversationId") conversationId: Long,
    ): ApiResponse<Unit>
}
