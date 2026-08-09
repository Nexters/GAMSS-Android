package com.gamss.android.data.remote.chattingsearch

import com.gamss.android.data.remote.chattingsearch.model.response.ChattingRoomSearchResponse
import com.gamss.android.data.remote.model.response.ApiResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 지정한 제목 또는 채팅 내용에 검색어가 포함된 본인 대화방을 최신순으로 조회한다.
 * 현재 진행중인 대화를 대상으로 진행한다.
 */
internal interface ChattingRoomSearchService {

    @GET("/api/conversations/search")
    suspend fun searchChattingRooms(
        @Query("keyword") keyword: String,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): ApiResponse<ChattingRoomSearchResponse>
}
