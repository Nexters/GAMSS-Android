package com.gamss.android.domain.card

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.emotion.EmotionCharacter
import java.time.LocalDate
import java.time.YearMonth

interface CardRepository {

    /** 대화 생성일(KST) 기준으로 해당 날짜의 카드를 가져온다. */
    suspend fun getCardsByDate(date: LocalDate): AppResult<List<Card>>

    /** 해당 달(KST)에 생성된 카드를 요약 없이 날짜·순번·감정만 가져온다. */
    suspend fun getCardsByMonth(yearMonth: YearMonth): AppResult<List<CardEntry>>

    /** 종료된 채팅방에만 만들 수 있고 방당 한 번만 성공한다. */
    suspend fun createCard(
        conversationId: Long,
        character: EmotionCharacter,
        summary: String,
    ): AppResult<Card>

    /** 카드 한 장과 카드가 나온 채팅방을 함께 삭제한다. 되돌릴 수 없다. */
    suspend fun deleteCard(cardId: Long): AppResult<Unit>
}
