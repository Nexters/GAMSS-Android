package com.gamss.android.domain.card

import com.gamss.android.core.common.AppResult
import java.time.LocalDate

interface CardQueryRepository {

    /** 해당 날짜(KST)에 생성된 카드 목록을 가져온다. */
    suspend fun getCardsByDate(date: LocalDate): AppResult<List<Card>>
}
