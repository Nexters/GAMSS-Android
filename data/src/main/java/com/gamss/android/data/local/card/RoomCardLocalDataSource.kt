package com.gamss.android.data.local.card

import com.gamss.android.data.local.card.model.CardEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RoomCardLocalDataSource @Inject constructor(
    private val cardDao: CardDao,
) : CardLocalDataSource {

    override suspend fun findById(cardId: Long): CardEntity? =
        cardDao.findById(cardId)

    override suspend fun upsert(card: CardEntity) {
        cardDao.upsert(card)
    }

    override suspend fun deleteAll() {
        cardDao.deleteAll()
    }
}
