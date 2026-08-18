package com.gamss.android.domain.card

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.emotion.EmotionCharacter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DeleteCardUseCaseTest {

    private class RecordingRepository : CardRepository {
        var deletedCardId: Long? = null
            private set

        override suspend fun getCardsByDate(date: LocalDate): AppResult<List<Card>> = error("사용하지 않음")

        override suspend fun createCard(
            conversationId: Long,
            character: EmotionCharacter,
            summary: String,
        ): AppResult<Card> = error("사용하지 않음")

        override suspend fun deleteAllCards(): AppResult<Unit> = error("사용하지 않음")

        override suspend fun deleteCard(cardId: Long): AppResult<Unit> {
            deletedCardId = cardId
            return AppResult.Success(Unit)
        }

        override suspend fun deleteCardsByEmotion(character: EmotionCharacter): AppResult<Unit> =
            error("사용하지 않음")
    }

    @Test
    fun `카드 삭제를 리포지토리에 위임한다`() = runBlocking {
        val repository = RecordingRepository()

        val result = DeleteCardUseCase(repository)(1L)

        assertEquals(1L, repository.deletedCardId)
        assertEquals(AppResult.Success(Unit), result)
    }
}
