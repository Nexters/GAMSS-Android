package com.gamss.android.domain.card

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.emotion.EmotionCharacter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DeleteCardUseCaseTest {

    private class RecordingRepository : CardRepository {
        var deletedCardId: Long? = null

        override suspend fun createCard(
            conversationId: Long,
            character: EmotionCharacter,
            summary: String,
        ): AppResult<Card> = error("사용하지 않음")

        override suspend fun deleteCard(cardId: Long): AppResult<Unit> {
            deletedCardId = cardId
            return AppResult.Success(Unit)
        }
    }

    @Test
    fun 카드_ID를_리포지토리에_그대로_위임한다() = runBlocking {
        val repository = RecordingRepository()

        val result = DeleteCardUseCase(repository)(CARD_ID)

        assertEquals(CARD_ID, repository.deletedCardId)
        assertEquals(AppResult.Success(Unit), result)
    }

    private companion object {
        const val CARD_ID = 42L
    }
}
