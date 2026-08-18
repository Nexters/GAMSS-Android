package com.gamss.android.domain.card

import com.gamss.android.core.common.AppResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DeleteCardUseCaseTest {

    private class RecordingRepository : FakeCardRepository() {
        var deletedCardId: Long? = null
            private set

        override suspend fun deleteCard(cardId: Long): AppResult<Unit> {
            deletedCardId = cardId
            return AppResult.Success(Unit)
        }
    }

    @Test
    fun `카드 삭제를 리포지토리에 위임한다`() = runBlocking {
        val repository = RecordingRepository()

        val result = DeleteCardUseCase(repository)(1L)

        assertEquals(1L, repository.deletedCardId)
        assertEquals(AppResult.Success(Unit), result)
    }
}
