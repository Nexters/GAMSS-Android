package com.gamss.android.data.remote.card

import com.gamss.android.data.remote.card.model.request.CreateCardRequest
import com.gamss.android.data.remote.card.model.response.CardResponse
import com.gamss.android.data.remote.model.response.ApiResponse
import retrofit2.http.Body
import retrofit2.http.POST

internal interface CardService {

    /** 종료된 채팅방에만 만들 수 있다. 대사는 서버가 생성한다. */
    @POST("/api/cards")
    suspend fun createCard(@Body request: CreateCardRequest): ApiResponse<CardResponse>
}
