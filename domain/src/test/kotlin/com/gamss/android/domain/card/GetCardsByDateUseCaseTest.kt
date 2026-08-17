package com.gamss.android.domain.card

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.emotion.EmotionCharacter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class GetCardsByDateUseCaseTest {

    @Test
    fun `선택한 날짜를 그대로 저장소에 전달한다`() = runBlocking {
        val date = LocalDate.of(2026, 8, 15)
        val expected = AppResult.Success(
            listOf(Card(EmotionCharacter.ANGER, "회의가 길어졌다", "오늘 많이 힘들었겠다")),
        )
        val repository = RecordingRepository(expected)

        val result = GetCardsByDateUseCase(repository)(date)

        assertEquals(date, repository.requestedDate)
        assertSame(expected, result)
    }

    private class RecordingRepository(
        private val result: AppResult<List<Card>>,
    ) : CardRepository {
        var requestedDate: LocalDate? = null
            private set

        override suspend fun getCardsByDate(date: LocalDate): AppResult<List<Card>> {
            requestedDate = date
            return result
        }

        override suspend fun getCardsByMonth(yearMonth: YearMonth): AppResult<List<CardEntry>> =
            AppResult.Success(emptyList())

        override suspend fun createCard(
            conversationId: Long,
            character: EmotionCharacter,
            summary: String,
        ): AppResult<Card> = error("날짜 조회 테스트에서 쓰지 않는다")
    }
}
