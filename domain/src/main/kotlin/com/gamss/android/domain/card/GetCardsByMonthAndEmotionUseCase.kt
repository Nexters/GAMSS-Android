package com.gamss.android.domain.card

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.emotion.EmotionCharacter
import com.gamss.android.domain.usecase.UseCase
import java.time.YearMonth
import javax.inject.Inject

/** 감정 없이 달만으로는 조회하지 않는다. */
data class MonthlyEmotionQuery(
    val character: EmotionCharacter,
    val yearMonth: YearMonth,
)

class GetCardsByMonthAndEmotionUseCase @Inject constructor(
    private val cardRepository: CardRepository,
) : UseCase<MonthlyEmotionQuery, AppResult<List<Card>>> {

    override suspend fun invoke(params: MonthlyEmotionQuery): AppResult<List<Card>> =
        cardRepository.getCardsByMonthAndEmotion(params.character, params.yearMonth)
}
