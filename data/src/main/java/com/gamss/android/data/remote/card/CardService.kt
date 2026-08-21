package com.gamss.android.data.remote.card

import com.gamss.android.data.remote.card.model.request.CreateCardRequest
import com.gamss.android.data.remote.card.model.response.CardDeleteResponse
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

    /** 최신순이고 페이지네이션이 없다. 한 달 한 감정 분량을 한 번에 준다. */
    @GET("/api/cards/monthly/emotions/{emotion}")
    suspend fun getCardsByMonthAndEmotion(
        @Path("emotion") emotion: String,
        @Query("yearMonth") yearMonth: String,
    ): ApiResponse<List<CardResponse>>

    /** 종료된 채팅방에만 만들 수 있다. 대사는 서버가 생성한다. */
    @POST("/api/cards")
    suspend fun createCard(@Body request: CreateCardRequest): ApiResponse<CardResponse>

    /** 모든 카드와 카드가 나온 채팅방을 함께 삭제한다. */
    @DELETE("/api/cards")
    suspend fun deleteAllCards(): ApiResponse<CardDeleteResponse>

    /** 카드 한 장과 카드가 나온 채팅방을 함께 삭제한다. envelope 의 success 만 보고 data 는 쓰지 않는다. */
    @DELETE("/api/cards/{cardId}")
    suspend fun deleteCard(@Path("cardId") cardId: Long): ApiResponse<Unit>

    /** 해당 감정인 카드와 카드가 나온 채팅방을 모두 함께 삭제한다. */
    @DELETE("/api/cards/emotions/{emotion}")
    suspend fun deleteCardsByEmotion(@Path("emotion") emotion: String): ApiResponse<CardDeleteResponse>
}
