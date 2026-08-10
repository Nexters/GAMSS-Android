package com.gamss.android.domain.repository

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.card.Card

interface CardRepository {
    suspend fun getCard(cardId: Long): AppResult<Card>
}
