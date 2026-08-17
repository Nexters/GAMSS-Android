package com.gamss.android.domain.card

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.usecase.UseCase
import javax.inject.Inject

/** 카드 한 장을 삭제한다. */
class DeleteCardUseCase @Inject constructor(
    private val cardRepository: CardRepository,
) : UseCase<Long, AppResult<Unit>> {

    override suspend fun invoke(params: Long): AppResult<Unit> = cardRepository.deleteCard(params)
}
