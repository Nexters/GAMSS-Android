package com.gamss.android.domain.card

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.emotion.EmotionCharacter

interface CardRepository {

    /** 종료된 채팅방에만 만들 수 있고 방당 한 번만 성공한다. */
    suspend fun createCard(
        conversationId: Long,
        character: EmotionCharacter,
        summary: String,
    ): AppResult<Card>

    /** 해당 카드와 카드가 나온 채팅방을 함께 삭제한다. 되돌릴 수 없다. */
    suspend fun deleteCard(cardId: Long): AppResult<Unit>
}
