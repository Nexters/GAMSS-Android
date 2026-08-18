package com.gamss.android.domain.card

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.emotion.EmotionCharacter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CreateCardUseCaseTest {

    private class RecordingRepository : CardRepository {
        var sentSummary: String? = null
            private set

        override suspend fun getCardsByDate(date: LocalDate): AppResult<List<Card>> =
            error("카드 생성 테스트에서 쓰지 않는다")

        override suspend fun getCardsByMonth(yearMonth: YearMonth): AppResult<List<CardEntry>> =
            error("카드 생성 테스트에서 쓰지 않는다")

        override suspend fun createCard(
            conversationId: Long,
            character: EmotionCharacter,
            summary: String,
        ): AppResult<Card> {
            sentSummary = summary
            return AppResult.Success(
                Card(
                    id = 1L,
                    conversationId = conversationId,
                    character = character,
                    emotionLabel = character.displayName,
                    summary = summary,
                    message = "대사",
                    date = LocalDate.of(2026, 8, 15),
                ),
            )
        }

        override suspend fun deleteAllCards(): AppResult<Unit> = error("사용하지 않음")

        override suspend fun deleteCard(cardId: Long): AppResult<Unit> = error("사용하지 않음")

        override suspend fun deleteCardsByEmotion(character: EmotionCharacter): AppResult<Unit> =
            error("사용하지 않음")
    }

    @Test
    fun 상한을_넘는_요약은_잘라서_보낸다() = runBlocking {
        val repository = RecordingRepository()

        val result = CreateCardUseCase(repository)(
            CreateCardUseCase.Params(
                conversationId = 1L,
                character = EmotionCharacter.ANGER,
                summary = "가".repeat(MAX_CARD_SUMMARY_LENGTH + 50),
            ),
        )

        assertEquals(MAX_CARD_SUMMARY_LENGTH, repository.sentSummary?.length)
        assertTrue(result is AppResult.Success)
    }

    @Test
    fun 앞뒤_공백을_정리해서_보낸다() = runBlocking {
        val repository = RecordingRepository()

        CreateCardUseCase(repository)(
            CreateCardUseCase.Params(
                conversationId = 1L,
                character = EmotionCharacter.SADNESS,
                summary = "  오늘 억울한 일이 있었다  ",
            ),
        )

        assertEquals("오늘 억울한 일이 있었다", repository.sentSummary)
    }
}
