package com.gamss.android.domain.card

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.emotion.EmotionCharacter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class GetCardUseCaseTest {

    private class RecordingRepository : CardRepository {
        var requestedCardId: Long? = null
            private set

        val card = Card(
            character = EmotionCharacter.ANGER,
            summary = "요약",
            message = "대사",
            id = 7L,
        )

        override suspend fun createCard(
            conversationId: Long,
            character: EmotionCharacter,
            summary: String,
        ): AppResult<Card> =
            AppResult.Failure(UnsupportedOperationException("not used"))

        override suspend fun getCard(cardId: Long): AppResult<Card> {
            requestedCardId = cardId
            return AppResult.Success(card)
        }

        override suspend fun clearCachedCards(): AppResult<Unit> =
            AppResult.Failure(UnsupportedOperationException("not used"))
    }

    @Test
    fun cardId를_repository에_그대로_위임한다() = runBlocking {
        val repository = RecordingRepository()

        val result = GetCardUseCase(repository)(7L)

        assertEquals(7L, repository.requestedCardId)
        assertSame(repository.card, (result as AppResult.Success).data)
    }
}
