package com.gamss.android.data.remote.card

import com.gamss.android.data.remote.card.model.request.CreateCardRequest
import com.gamss.android.data.remote.card.model.response.CardCalendarResponse
import com.gamss.android.data.remote.card.model.response.CardResponse
import com.gamss.android.data.remote.model.response.ApiResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

internal interface CardService {

    @GET("/api/cards")
    suspend fun getCardsByDate(@Query("date") date: String): ApiResponse<List<CardResponse>>

    /** 그 달(KST)의 날짜별 대표 감정 목록만 준다. 한 줄 요약 등 상세는 [getCardsByDate] 로 따로 받는다. */
    @GET("/api/cards/monthly")
    suspend fun getCardsByMonth(
        @Query("yearMonth") yearMonth: String,
    ): ApiResponse<List<CardCalendarResponse>>

    /** 종료된 채팅방에만 만들 수 있다. 대사는 서버가 생성한다. */
    @POST("/api/cards")
    suspend fun createCard(@Body request: CreateCardRequest): ApiResponse<CardResponse>

    /** 카드 한 장과 카드가 나온 채팅방을 함께 삭제한다. envelope 의 success 만 보고 data 는 쓰지 않는다. */
    @DELETE("/api/cards/{cardId}")
    suspend fun deleteCard(@Path("cardId") cardId: Long): ApiResponse<Unit>
}
