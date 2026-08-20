package com.gamss.android.domain.card

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.emotion.EmotionCharacter
import java.time.LocalDate
import java.time.YearMonth

interface CardRepository {

    /**
     * 대화 생성일(KST) 기준으로 해당 날짜의 카드를 가져온다.
     * 그 날짜가 캐시에 있으면 캐시를, 없으면 서버에서 가져와 캐시에 저장한다.
     */
    suspend fun getCardsByDate(date: LocalDate): AppResult<List<Card>>

    /** 해당 달(KST)에 생성된 그 감정의 카드를 내용까지 가져온다. 오래된 순이다. */
    suspend fun getCardsByMonthAndEmotion(
        character: EmotionCharacter,
        yearMonth: YearMonth,
    ): AppResult<List<Card>>

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

    /**
     * 서버엔 아무 요청도 보내지 않고 기기에 남은 카드 캐시만 지운다.
     * 로그아웃·탈퇴처럼 계정을 벗어나는 시점, 캐시가 서버 상태와 어긋난 것으로 확인된 시점에 쓴다.
     * 실패해도 던지지 않고 기록만 하므로 호출부가 따로 예외를 막지 않아도 된다.
     */
    suspend fun clearCache()
}
