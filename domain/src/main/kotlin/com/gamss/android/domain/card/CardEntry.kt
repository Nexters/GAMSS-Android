package com.gamss.android.domain.card

import com.gamss.android.domain.emotion.EmotionCharacter
import java.time.LocalDate

/** 월별 응답에는 카드 식별자가 없어 [date] 와 그날 순번 [indexInDate] 가 카드의 신원이다. */
data class CardEntry(
    val date: LocalDate,
    val indexInDate: Int,
    val character: EmotionCharacter,
)
