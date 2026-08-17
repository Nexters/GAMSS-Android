package com.gamss.android.domain.card

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.emotion.EmotionCharacter
import java.time.LocalDate

interface CardRepository {

    /** 대화 생성일(KST) 기준으로 해당 날짜의 카드를 가져온다. */
    suspend fun getCardsByDate(date: LocalDate): AppResult<List<Card>>

    /** 종료된 채팅방에만 만들 수 있고 방당 한 번만 성공한다. */
    suspend fun createCard(
        conversationId: Long,
        character: EmotionCharacter,
        summary: String,
    ): AppResult<Card>

    /** 모든 카드와 카드가 나온 채팅방을 함께 삭제한다. 되돌릴 수 없다. */
    suspend fun deleteAllCards(): AppResult<Unit>

    /** 카드 한 장과 카드가 나온 채팅방을 함께 삭제한다. 되돌릴 수 없다. */
    suspend fun deleteCard(cardId: Long): AppResult<Unit>

    /** 해당 감정인 카드와 카드가 나온 채팅방을 모두 함께 삭제한다. 되돌릴 수 없다. */
    suspend fun deleteCardsByEmotion(character: EmotionCharacter): AppResult<Unit>
}
