package com.gamss.android.domain.card

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.usecase.UseCase
import java.time.LocalDate
import javax.inject.Inject

class GetCardsByDateUseCase @Inject constructor(
    private val cardRepository: CardRepository,
) : UseCase<LocalDate, AppResult<List<Card>>> {

    override suspend fun invoke(params: LocalDate): AppResult<List<Card>> =
        cardRepository.getCardsByDate(params)
}
