package com.gamss.android.data.repository

import com.gamss.android.core.common.AppResult
import com.gamss.android.data.local.card.CardLocalDataSource
import com.gamss.android.data.local.card.model.toDomain
import com.gamss.android.data.remote.card.CardService
import com.gamss.android.data.remote.card.model.request.CreateCardRequest
import com.gamss.android.data.remote.card.model.response.toDomain
import com.gamss.android.data.remote.card.model.response.toDomainOrNull
import com.gamss.android.data.remote.card.model.response.toEntity
import com.gamss.android.data.remote.emotion.toServerEmotionType
import com.gamss.android.data.remote.runCatchingApiCall
import com.gamss.android.data.remote.throwIfFailed
import com.gamss.android.domain.card.Card
import com.gamss.android.domain.card.CardEntry
import com.gamss.android.domain.card.CardNotRetryableException
import com.gamss.android.domain.card.CardRepository
import com.gamss.android.domain.emotion.EmotionCharacter
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class CardRepositoryImpl @Inject constructor(
    private val cardService: CardService,
    private val cardLocalDataSource: CardLocalDataSource,
) : CardRepository {

    override suspend fun getCardsByDate(date: LocalDate): AppResult<List<Card>> = runCatchingApiCall {
        val response = cardService.getCardsByDate(date.toString())
        response.throwIfFailed()
        checkNotNull(response.data) { "No available card data" }.mapNotNull { it.toDomainOrNull() }
    }

    // YearMonth.toString() 이 서버가 요구하는 yyyy-MM 그대로다.
    override suspend fun getCardsByMonth(yearMonth: YearMonth): AppResult<List<CardEntry>> = runCatchingApiCall {
        val response = cardService.getCardsByMonth(yearMonth.toString())
        response.throwIfFailed()
        checkNotNull(response.data) { "No available card data" }
            .flatMap { it.toDomain() }
    }

    /** 캐시에 있으면 캐시를 그대로 쓰고, 없을 때만 서버를 호출해 다음 조회를 위해 캐시에 남긴다. */
    override suspend fun getCard(cardId: Long): AppResult<Card> {
        cardLocalDataSource.findById(cardId)?.let { cached ->
            return AppResult.Success(cached.toDomain())
        }
        return runCatchingApiCall {
            val response = cardService.getCard(cardId)
            response.throwIfFailed()
            val data = checkNotNull(response.data) { "No available card data" }
            cardLocalDataSource.upsert(data.toEntity())
            checkNotNull(data.toDomainOrNull()) { "No available card data" }
        }
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
            checkNotNull(response.data) { "No available card data" }
                .toDomain(requestedCharacter = character, fallbackDate = LocalDate.now())
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

    override suspend fun deleteAllCards(): AppResult<Unit> = runCatchingApiCall {
        val response = cardService.deleteAllCards()
        response.throwIfFailed()
        checkNotNull(response.data?.deletedCount) { "No deleted card count" }
        Unit
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

    override suspend fun deleteCardsByEmotion(character: EmotionCharacter): AppResult<Unit> = runCatchingApiCall {
        val response = cardService.deleteCardsByEmotion(character.toServerEmotionType())
        response.throwIfFailed()
        checkNotNull(response.data?.deletedCount) { "No deleted card count" }
        Unit
    }
}
