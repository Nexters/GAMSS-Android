package com.gamss.android.domain.card

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.assertSuccess
import com.gamss.android.domain.repository.CardRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetCardUseCaseTest {

    @Test
    fun `cardId로 카드 조회를 위임한다`() = runBlocking {
        val repository = FakeCardRepository()
        val useCase = GetCardUseCase(repository)

        val result = useCase(7).assertSuccess()

        assertEquals(7L, repository.requestedCardId)
        assertEquals(7L, result.id)
    }

    private class FakeCardRepository : CardRepository {
        var requestedCardId: Long? = null
            private set

        override suspend fun getCard(cardId: Long): AppResult<Card> {
            requestedCardId = cardId
            return AppResult.Success(
                Card(
                    id = cardId,
                    conversationId = 1,
                    emotion = "ANGER",
                    emotionLabel = "분노",
                    summary = "오늘 비가 와서 짜증나고 찝찝하다",
                    message = "얘 오늘 건들면 안 됨.",
                    date = "2026-07-23",
                ),
            )
        }
    }
}
