package com.gamss.android.domain.card

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.emotion.EmotionCharacter
import com.gamss.android.domain.usecase.UseCase
import java.time.YearMonth
import javax.inject.Inject

/** 보관함 한 칸이 한 번에 보는 범위. 감정 없이 달만으로는 조회하지 않는다. */
data class MonthlyEmotionQuery(
    val character: EmotionCharacter,
    val yearMonth: YearMonth,
)

/** 선택한 달(KST)에 생성된 그 감정의 카드를 내용까지 조회한다. */
class GetCardsByMonthAndEmotionUseCase @Inject constructor(
    private val cardRepository: CardRepository,
) : UseCase<MonthlyEmotionQuery, AppResult<List<Card>>> {

    override suspend fun invoke(params: MonthlyEmotionQuery): AppResult<List<Card>> =
        cardRepository.getCardsByMonthAndEmotion(params.character, params.yearMonth)
}
