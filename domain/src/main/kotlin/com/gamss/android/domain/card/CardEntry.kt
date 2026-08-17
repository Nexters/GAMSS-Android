package com.gamss.android.domain.card

import com.gamss.android.domain.emotion.EmotionCharacter
import java.time.LocalDate

/**
 * 월별 조회로 받은 카드 한 건. 어떤 감정 카드가 언제 있었는지만 담고 요약·대사는 없다.
 *
 * 월별 응답에 카드 식별자가 없어 [date] 와 그날 순번 [indexInDate] 가 카드의 신원이다. 상세가
 * 필요하면 [date] 로 날짜별 조회해 [indexInDate] 번째 카드를 쓴다. 두 응답 모두 생성순이고 알 수 없는
 * 감정을 같은 기준으로 걸러내므로, 순번은 걸러낸 뒤를 기준으로 센다.
 */
data class CardEntry(
    val date: LocalDate,
    val indexInDate: Int,
    val character: EmotionCharacter,
)
