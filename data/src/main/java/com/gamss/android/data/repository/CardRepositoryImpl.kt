package com.gamss.android.data.repository

import android.util.Log
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
import com.gamss.android.domain.card.CardNotRetryableException
import com.gamss.android.domain.card.CardRepository
import com.gamss.android.domain.emotion.EmotionCharacter
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

private const val TAG = "CardRepositoryImpl"

@Singleton
internal class CardRepositoryImpl @Inject constructor(
    private val cardService: CardService,
    private val cardLocalDataSource: CardLocalDataSource,
) : CardRepository {

    /**
     * 캐시를 읽지도 쓰지도 않는다. 캐시는 (감정, 달) 단위로만 채워지므로, 날짜로 걸러 읽으면 그
     * 날의 카드가 다 들어 있다는 보장이 없다. 한 장이라도 있으면 완전하다고 오해해 서버를
     * 건너뛰게 되므로, 이 조회는 늘 서버를 본다.
     */
    override suspend fun getCardsByDate(date: LocalDate): AppResult<List<Card>> = runCatchingApiCall {
        val response = cardService.getCardsByDate(date.toString())
        response.throwIfFailed()
        checkNotNull(response.data) { "No available card data" }.mapNotNull { it.toDomainOrNull() }
    }

    override suspend fun getCardsByMonthAndEmotion(
        character: EmotionCharacter,
        yearMonth: YearMonth,
    ): AppResult<List<Card>> {
        val serverEmotion = character.toServerEmotionType()
        // 이 조회만 캐시를 채우므로, 한 행이라도 있으면 그 (감정, 달) 은 통째로 받아 둔 것이다.
        runCatching {
            cardLocalDataSource
                .findByEmotionAndMonth(serverEmotion, yearMonth)
                .map { it.toDomain() }
                .sortedOldestFirst()
        }
            .onFailure { throwable ->
                if (throwable is CancellationException) throw throwable

                Log.w(
                    TAG,
                    "카드 캐시 조회에 실패해 서버 조회로 대체합니다. emotion=$serverEmotion, yearMonth=$yearMonth",
                    throwable,
                )
            }
            .getOrNull()
            ?.takeIf { it.isNotEmpty() }
            ?.let { return AppResult.Success(it) }

        return runCatchingApiCall {
            val response = cardService.getCardsByMonthAndEmotion(
                emotion = serverEmotion,
                yearMonth = yearMonth.toString(),
            )
            response.throwIfFailed()

            val validCards = checkNotNull(response.data) { "No available card data" }
                .mapNotNull { raw -> raw.toDomainOrNull()?.let { raw to it } }

            cardLocalDataSource.upsertAll(validCards.map { (raw, _) -> raw.toEntity() })

            validCards.map { (_, card) -> card }.sortedOldestFirst()
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
            // 새 카드가 그 날짜 캐시에 없는 채로 남으면 그 날은 다시 열어도 계속 예전 목록만 보인다.
            is AppResult.Success -> {
                cardLocalDataSource.deleteAll()
                result
            }
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
        cardLocalDataSource.deleteAll()
    }

    /**
     * 지운 카드가 어느 감정 칸·어느 달에 있었는지 이 메서드는 모른다. 그 칸만 골라 비우는 대신
     * 캐시 전체를 비워 다음 조회 때 다시 채우게 한다.
     */
    override suspend fun deleteCard(cardId: Long): AppResult<Unit> {
        val result = runCatchingApiCall {
            cardService.deleteCard(cardId).throwIfFailed()
        }
        return when (result) {
            is AppResult.Success -> {
                cardLocalDataSource.deleteAll()
                AppResult.Success(Unit)
            }
            // 이미 지워진 카드면 목표는 달성된 상태다. 실패로 흘리면 재시도가 영원히 같은 오류를 받는다.
            is AppResult.Failure ->
                if (result.throwable.hasErrorCode(CARD_ALREADY_DELETED)) {
                    cardLocalDataSource.deleteAll()
                    AppResult.Success(Unit)
                } else {
                    result
                }
        }
    }

    override suspend fun deleteCardsByEmotion(character: EmotionCharacter): AppResult<Unit> =
        runCatchingApiCall {
            val response = cardService.deleteCardsByEmotion(character.toServerEmotionType())
            response.throwIfFailed()
            checkNotNull(response.data?.deletedCount) { "No deleted card count" }
            cardLocalDataSource.deleteAll()
        }

    /**
     * 실패해도 던지지 않고 기록만 한다. 로그아웃 중 세션 정리를 무너뜨리거나, Orbit intent 안에서
     * 전역 예외 핸들러 없이 그대로 크래시로 번지는 걸 호출부마다 따로 막지 않아도 되게 한다.
     */
    override suspend fun clearCache() {
        runCatching { cardLocalDataSource.deleteAll() }
            .onFailure { throwable ->
                if (throwable is CancellationException) throw throwable
                runCatching { Log.w(TAG, "카드 캐시 삭제에 실패했습니다.", throwable) }
            }
    }
}

/**
 * 캐시와 서버, 두 경로가 같은 목록에 같은 순서를 내야 한다. 서버 정렬을 가정하지 않고 여기서
 * 확정한다.
 */
private fun List<Card>.sortedOldestFirst(): List<Card> =
    sortedWith(compareBy({ it.date }, { it.id }))
