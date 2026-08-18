package com.gamss.android.domain.card

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.usecase.UseCase
import java.time.YearMonth
import javax.inject.Inject

/** 선택한 달(KST)에 생성된 카드를 요약 없이 조회한다. */
class GetCardsByMonthUseCase @Inject constructor(
    private val cardRepository: CardRepository,
) : UseCase<YearMonth, AppResult<List<CardEntry>>> {

    override suspend fun invoke(params: YearMonth): AppResult<List<CardEntry>> =
        cardRepository.getCardsByMonth(params)
}
