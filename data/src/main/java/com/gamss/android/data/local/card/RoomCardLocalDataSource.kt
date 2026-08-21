package com.gamss.android.data.local.card

import com.gamss.android.data.local.card.model.CardEntity
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RoomCardLocalDataSource @Inject constructor(
    private val cardDao: CardDao,
) : CardLocalDataSource {

    override suspend fun findByDate(date: LocalDate): List<CardEntity> =
        cardDao.findByDate(date.toString())

    override suspend fun findByEmotionAndMonth(emotion: String, yearMonth: YearMonth): List<CardEntity> =
        cardDao.findByEmotionAndMonth(emotion, yearMonth.toString())

    override suspend fun upsertAll(cards: List<CardEntity>) {
        cardDao.upsertAll(cards)
    }

    override suspend fun deleteAll() {
        cardDao.deleteAll()
    }
}
