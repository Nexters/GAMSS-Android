package com.gamss.android.data.local.card

import com.gamss.android.data.local.card.model.CardEntity
import java.time.LocalDate

internal interface CardLocalDataSource {
    suspend fun findByDate(date: LocalDate): List<CardEntity>
    suspend fun upsertAll(cards: List<CardEntity>)
    suspend fun deleteAll()
}
