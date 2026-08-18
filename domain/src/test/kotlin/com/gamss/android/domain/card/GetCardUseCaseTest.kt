package com.gamss.android.domain.card

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.emotion.EmotionCharacter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.time.LocalDate

class GetCardUseCaseTest {

    private class RecordingRepository : FakeCardRepository() {
        var requestedCardId: Long? = null
            private set

        val card = Card(
            id = 7L,
            conversationId = 1L,
            character = EmotionCharacter.ANGER,
            emotionLabel = "분노",
            summary = "요약",
            message = "대사",
            date = LocalDate.of(2026, 7, 23),
        )

        override suspend fun getCard(cardId: Long): AppResult<Card> {
            requestedCardId = cardId
            return AppResult.Success(card)
        }
    }

    @Test
    fun cardId를_repository에_그대로_위임한다() = runBlocking {
        val repository = RecordingRepository()

        val result = GetCardUseCase(repository)(7L)

        assertEquals(7L, repository.requestedCardId)
        assertSame(repository.card, (result as AppResult.Success).data)
    }
}
