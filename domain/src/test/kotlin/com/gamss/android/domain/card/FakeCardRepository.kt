package com.gamss.android.domain.card

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.emotion.EmotionCharacter
import java.time.LocalDate
import java.time.YearMonth

/** 테스트가 실제로 쓰는 메서드만 override 한다. 나머지는 호출되면 바로 드러나야 한다. */
internal open class FakeCardRepository : CardRepository {

    override suspend fun getCardsByDate(date: LocalDate): AppResult<List<Card>> =
        error("이 테스트에서 쓰지 않는다")

    override suspend fun getCardsByMonthAndEmotion(
        character: EmotionCharacter,
        yearMonth: YearMonth,
    ): AppResult<List<Card>> =
        error("이 테스트에서 쓰지 않는다")

    override suspend fun createCard(
        conversationId: Long,
        character: EmotionCharacter,
        summary: String,
    ): AppResult<Card> = error("이 테스트에서 쓰지 않는다")

    override suspend fun deleteAllCards(): AppResult<Unit> = error("이 테스트에서 쓰지 않는다")

    override suspend fun deleteCard(cardId: Long): AppResult<Unit> = error("이 테스트에서 쓰지 않는다")

    override suspend fun deleteCardsByEmotion(character: EmotionCharacter): AppResult<Unit> =
        error("이 테스트에서 쓰지 않는다")

    override suspend fun clearCache(): Unit = error("이 테스트에서 쓰지 않는다")
}
