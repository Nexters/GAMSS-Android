package com.gamss.android.domain.card

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.emotion.EmotionCharacter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class GetCardsByMonthUseCaseTest {

    @Test
    fun `선택한 달을 그대로 저장소에 전달한다`() = runBlocking {
        val yearMonth = YearMonth.of(2026, 8)
        val expected = AppResult.Success(
            listOf(CardEntry(LocalDate.of(2026, 8, 15), indexInDate = 0, character = EmotionCharacter.ANGER)),
        )
        val repository = RecordingRepository(expected)

        val result = GetCardsByMonthUseCase(repository)(yearMonth)

        assertEquals(yearMonth, repository.requestedMonth)
        assertSame(expected, result)
    }

    private class RecordingRepository(
        private val result: AppResult<List<CardEntry>>,
    ) : CardRepository {
        var requestedMonth: YearMonth? = null
            private set

        override suspend fun getCardsByDate(date: LocalDate): AppResult<List<Card>> =
            AppResult.Success(emptyList())

        override suspend fun getCardsByMonth(yearMonth: YearMonth): AppResult<List<CardEntry>> {
            requestedMonth = yearMonth
            return result
        }

        override suspend fun createCard(
            conversationId: Long,
            character: EmotionCharacter,
            summary: String,
        ): AppResult<Card> = error("월 조회 테스트에서 쓰지 않는다")
    }
}
