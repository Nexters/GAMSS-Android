package com.gamss.android.data.repository

import com.gamss.android.core.common.AppResult
import com.gamss.android.data.remote.card.CardService
import com.gamss.android.data.remote.card.model.request.CreateCardRequest
import com.gamss.android.data.remote.card.model.response.toDomain
import com.gamss.android.data.remote.emotion.toServerEmotionType
import com.gamss.android.domain.card.Card
import com.gamss.android.domain.card.CardAlreadyExistsException
import com.gamss.android.domain.card.CardRepository
import com.gamss.android.domain.emotion.EmotionCharacter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class CardRepositoryImpl @Inject constructor(
    private val cardService: CardService,
) : CardRepository {

    override suspend fun createCard(
        conversationId: Long,
        character: EmotionCharacter,
        summary: String,
    ): AppResult<Card> {
        val result = AppResult.of {
            val response = cardService.createCard(
                CreateCardRequest(
                    conversationId = conversationId,
                    emotion = character.toServerEmotionType(),
                    summary = summary,
                ),
            )
            checkNotNull(response.data) { "No available card data" }.toDomain(character)
        }
        return when (result) {
            is AppResult.Success -> result
            // 재시도해도 계속 409 이므로 호출부가 "다시 만들기"를 접을 수 있게 구분해서 알린다.
            is AppResult.Failure ->
                if (result.throwable.isConflict()) {
                    AppResult.Failure(CardAlreadyExistsException(result.throwable))
                } else {
                    result
                }
        }
    }
}
