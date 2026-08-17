package com.gamss.android.data.repository

import com.gamss.android.core.common.AppResult
import com.gamss.android.data.remote.card.CardService
import com.gamss.android.data.remote.card.model.request.CreateCardRequest
import com.gamss.android.data.remote.card.model.response.toDomain
import com.gamss.android.data.remote.card.model.response.toDomainOrNull
import com.gamss.android.data.remote.emotion.toServerEmotionType
import com.gamss.android.data.remote.runCatchingApiCall
import com.gamss.android.data.remote.throwIfFailed
import com.gamss.android.domain.card.Card
import com.gamss.android.domain.card.CardNotRetryableException
import com.gamss.android.domain.card.CardRepository
import com.gamss.android.domain.emotion.EmotionCharacter
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class CardRepositoryImpl @Inject constructor(
    private val cardService: CardService,
) : CardRepository {

    override suspend fun getCardsByDate(date: LocalDate): AppResult<List<Card>> = runCatchingApiCall {
        val response = cardService.getCardsByDate(date.toString())
        response.throwIfFailed()
        checkNotNull(response.data) { "No available card data" }.mapNotNull { it.toDomainOrNull() }
    }

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
            checkNotNull(response.data) { "No available card data" }.toDomain()
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
            // 이미 지워진 카드면 목표는 달성된 상태다. 실패로 흘리면 재시도가 영원히 같은 오류를 받는다.
            is AppResult.Failure ->
                if (result.throwable.hasErrorCode(CARD_ALREADY_DELETED)) {
                    AppResult.Success(Unit)
                } else {
                    result
                }
        }
    }
}
