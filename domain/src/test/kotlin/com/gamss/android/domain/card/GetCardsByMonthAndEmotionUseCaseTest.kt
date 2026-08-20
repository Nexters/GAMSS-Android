package com.gamss.android.domain.card

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.emotion.EmotionCharacter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class GetCardsByMonthAndEmotionUseCaseTest {

    @Test
    fun `고른 감정과 달을 그대로 저장소에 전달한다`() = runBlocking {
        val query = MonthlyEmotionQuery(EmotionCharacter.ANGER, YearMonth.of(2026, 8))
        val expected = AppResult.Success(
            listOf(
                Card(
                    id = 1L,
                    conversationId = 10L,
                    character = EmotionCharacter.ANGER,
                    emotionLabel = "분노",
                    summary = "요약",
                    message = "대사",
                    date = LocalDate.of(2026, 8, 15),
                ),
            ),
        )
        val repository = RecordingRepository(expected)

        val result = GetCardsByMonthAndEmotionUseCase(repository)(query)

        assertEquals(query.character, repository.requestedCharacter)
        assertEquals(query.yearMonth, repository.requestedMonth)
        assertSame(expected, result)
    }

    private class RecordingRepository(
        private val result: AppResult<List<Card>>,
    ) : FakeCardRepository() {
        var requestedCharacter: EmotionCharacter? = null
            private set
        var requestedMonth: YearMonth? = null
            private set

        override suspend fun getCardsByMonthAndEmotion(
            character: EmotionCharacter,
            yearMonth: YearMonth,
        ): AppResult<List<Card>> {
            requestedCharacter = character
            requestedMonth = yearMonth
            return result
        }
    }
}
