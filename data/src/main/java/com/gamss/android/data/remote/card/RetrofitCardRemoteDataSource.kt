package com.gamss.android.data.remote.card

import com.gamss.android.data.remote.card.model.response.CardResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RetrofitCardRemoteDataSource @Inject constructor(
    private val cardService: CardService,
) : CardRemoteDataSource {

    override suspend fun getCard(cardId: Long): CardResponse {
        val response = cardService.getCard(cardId)
        return checkNotNull(response.data) { "No available card data" }
    }
}
