package com.gamss.android.domain.card

import com.gamss.android.core.common.AppResult
import java.time.LocalDate
import java.time.YearMonth

interface CardQueryRepository {

    /** 해당 날짜(KST)에 생성된 카드 목록을 가져온다. */
    suspend fun getCardsByDate(date: LocalDate): AppResult<List<Card>>

    /** 해당 달(KST)에 생성된 카드를 요약 없이 날짜·순번·감정만 가져온다. */
    suspend fun getCardsByMonth(yearMonth: YearMonth): AppResult<List<CardEntry>>
}
