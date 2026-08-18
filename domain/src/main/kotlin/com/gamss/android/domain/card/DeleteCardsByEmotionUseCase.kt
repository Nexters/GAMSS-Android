package com.gamss.android.domain.card

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.emotion.EmotionCharacter
import com.gamss.android.domain.usecase.UseCase
import javax.inject.Inject

class DeleteCardsByEmotionUseCase @Inject constructor(
    private val cardRepository: CardRepository,
) : UseCase<EmotionCharacter, AppResult<Unit>> {
    override suspend fun invoke(params: EmotionCharacter): AppResult<Unit> =
        cardRepository.deleteCardsByEmotion(params)
}
