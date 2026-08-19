package com.gamss.android.data.remote.conversation

import com.gamss.android.data.remote.conversation.model.request.SaveMessageRequest
import com.gamss.android.data.remote.conversation.model.request.UpdateConversationTitleRequest
import com.gamss.android.data.remote.conversation.model.response.ChattingRoomSearchResponse
import com.gamss.android.data.remote.conversation.model.response.ConversationDetailResponse
import com.gamss.android.data.remote.conversation.model.response.ConversationResponse
import com.gamss.android.data.remote.conversation.model.response.SaveMessageResponse
import com.gamss.android.data.remote.model.response.ApiResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

internal interface ConversationService {

    @GET("/api/conversations/incomplete")
    suspend fun getIncompleteConversations(): ApiResponse<List<ConversationResponse>>

    @POST("/api/conversations/messages")
    suspend fun saveMessage(@Body request: SaveMessageRequest): ApiResponse<SaveMessageResponse>

    @GET("/api/conversations/{conversationId}")
    suspend fun getConversation(@Path("conversationId") conversationId: Long): ApiResponse<ConversationDetailResponse>

    @PATCH("/api/conversations/{conversationId}/title")
    suspend fun updateTitle(
        @Path("conversationId") conversationId: Long,
        @Body request: UpdateConversationTitleRequest,
    ): ApiResponse<ConversationResponse>

    @POST("/api/conversations/{conversationId}/end")
    suspend fun endConversation(
        @Path("conversationId") conversationId: Long,
    ): ApiResponse<Unit>

    /** 종료 여부와 무관하게 지울 수 있다. envelope 의 success 만 보고 data 는 쓰지 않는다. */
    @DELETE("/api/conversations/{conversationId}")
    suspend fun deleteConversation(
        @Path("conversationId") conversationId: Long,
    ): ApiResponse<Unit>

    /**
     * 지정한 제목 또는 채팅 내용에 검색어가 포함된 본인 대화방을 최신순으로 조회한다.
     * 현재 진행중인 대화를 대상으로 진행한다.
     */
    @GET("/api/conversations/search")
    suspend fun searchChattingRooms(
        @Query("keyword") keyword: String,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): ApiResponse<ChattingRoomSearchResponse>
}
