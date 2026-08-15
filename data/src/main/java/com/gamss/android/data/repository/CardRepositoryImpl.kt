package com.gamss.android.data.repository

import com.gamss.android.core.common.AppResult
import com.gamss.android.data.remote.card.CardService
import com.gamss.android.data.remote.card.model.request.CreateCardRequest
import com.gamss.android.data.remote.card.model.response.toDomainOrNull
import com.gamss.android.data.remote.emotion.toServerEmotionType
import com.gamss.android.data.remote.runCatchingApiCall
import com.gamss.android.data.remote.throwIfFailed
import com.gamss.android.domain.card.Card
import com.gamss.android.domain.card.CardNotRetryableException
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
        val result = runCatchingApiCall {
            val response = cardService.createCard(
                CreateCardRequest(
                    conversationId = conversationId,
                    emotion = character.toServerEmotionType(),
                    summary = summary,
                ),
            )
            response.throwIfFailed()
            checkNotNull(response.data?.toDomainOrNull()) { "No available valid card data" }
        }
        return when (result) {
            is AppResult.Success -> result
            // 재시도해도 계속 409 이므로 호출부가 "다시 만들기"를 접을 수 있게 구분해서 알린다.
            is AppResult.Failure ->
                if (result.throwable.hasErrorCode(CARD_ALREADY_EXISTS)) {
                    AppResult.Failure(CardNotRetryableException.AlreadyExists(result.throwable))
                } else {
                    result
                }
        }
    }

    override suspend fun deleteCard(cardId: Long): AppResult<Unit> {
        val result = runCatchingApiCall {
            cardService.deleteCard(cardId).throwIfFailed()
        }
        return when (result) {
            is AppResult.Success -> AppResult.Success(Unit)
            // 응답만 유실된 재시도일 수 있다. 이미 삭제됨도 사용자가 의도한 최종 상태다.
            is AppResult.Failure ->
                if (result.throwable.hasErrorCode(CARD_ALREADY_DELETED)) AppResult.Success(Unit) else result
        }
    }
}
