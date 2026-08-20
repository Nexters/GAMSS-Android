package com.gamss.android.domain.card

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.emotion.EmotionCharacter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.time.LocalDate

class GetCardsByDateUseCaseTest {

    @Test
    fun `선택한 날짜를 그대로 저장소에 전달한다`() = runBlocking {
        val date = LocalDate.of(2026, 8, 15)
        val expected = AppResult.Success(
            listOf(
                Card(
                    id = 1L,
                    conversationId = 10L,
                    character = EmotionCharacter.ANGER,
                    emotionLabel = "분노",
                    summary = "회의가 길어졌다",
                    message = "오늘 많이 힘들었겠다",
                    date = LocalDate.of(2026, 8, 15),
                ),
            ),
        )
        val repository = RecordingRepository(expected)

        val result = GetCardsByDateUseCase(repository)(date)

        assertEquals(date, repository.requestedDate)
        assertSame(expected, result)
    }

    private class RecordingRepository(
        private val result: AppResult<List<Card>>,
    ) : FakeCardRepository() {
        var requestedDate: LocalDate? = null
            private set

        override suspend fun getCardsByDate(date: LocalDate): AppResult<List<Card>> {
            requestedDate = date
            return result
        }
    }
}
