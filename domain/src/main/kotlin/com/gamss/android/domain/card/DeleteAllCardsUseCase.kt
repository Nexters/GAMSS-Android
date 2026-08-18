package com.gamss.android.domain.card

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.usecase.NoParamUseCase
import javax.inject.Inject

class DeleteAllCardsUseCase @Inject constructor(
    private val cardRepository: CardRepository,
) : NoParamUseCase<AppResult<Unit>> {
    override suspend fun invoke(): AppResult<Unit> = cardRepository.deleteAllCards()
}
