package com.gamss.android.data.local.card

import com.gamss.android.data.local.card.model.CardEntity
import java.time.YearMonth

internal interface CardLocalDataSource {
    suspend fun findByEmotionAndMonth(emotion: String, yearMonth: YearMonth): List<CardEntity>
    suspend fun upsertAll(cards: List<CardEntity>)
    suspend fun deleteAll()
}
