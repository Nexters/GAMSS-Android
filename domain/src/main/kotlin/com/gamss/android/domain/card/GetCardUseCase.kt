package com.gamss.android.domain.card

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.repository.CardRepository
import com.gamss.android.domain.usecase.UseCase
import javax.inject.Inject

class GetCardUseCase @Inject constructor(
    private val cardRepository: CardRepository,
) : UseCase<Long, AppResult<Card>> {

    override suspend fun invoke(params: Long): AppResult<Card> =
        cardRepository.getCard(cardId = params)
}
