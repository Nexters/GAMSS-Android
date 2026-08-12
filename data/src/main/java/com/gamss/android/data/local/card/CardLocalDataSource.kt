package com.gamss.android.data.local.card

import com.gamss.android.data.local.card.model.CardEntity

internal interface CardLocalDataSource {
    suspend fun findById(cardId: Long): CardEntity?
    suspend fun upsert(card: CardEntity)
    suspend fun deleteAll()
}
