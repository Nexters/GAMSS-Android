package com.gamss.android.domain.card

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.emotion.EmotionCharacter
import com.gamss.android.domain.usecase.UseCase
import javax.inject.Inject

/** 서버가 받는 카드 요약의 최대 길이. */
const val MAX_CARD_SUMMARY_LENGTH = 1000

class CreateCardUseCase @Inject constructor(
    private val cardRepository: CardRepository,
) : UseCase<CreateCardUseCase.Params, AppResult<Card>> {

    data class Params(
        val conversationId: Long,
        val character: EmotionCharacter,
        val summary: String,
    )

    override suspend fun invoke(params: Params): AppResult<Card> = cardRepository.createCard(
        conversationId = params.conversationId,
        character = params.character,
        // 상한을 넘기면 서버가 INVALID_INPUT 으로 거절한다.
        summary = params.summary.trim().take(MAX_CARD_SUMMARY_LENGTH),
    )
}
