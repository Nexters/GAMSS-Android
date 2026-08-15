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
}
