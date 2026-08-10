package com.gamss.android.data.remote.card

import com.gamss.android.data.remote.card.model.response.CardResponse

internal interface CardRemoteDataSource {
    suspend fun getCard(cardId: Long): CardResponse
}
