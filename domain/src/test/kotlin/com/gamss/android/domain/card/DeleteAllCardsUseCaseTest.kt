package com.gamss.android.domain.card

import com.gamss.android.core.common.AppResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DeleteAllCardsUseCaseTest {

    private class RecordingRepository : FakeCardRepository() {
        var deleteAllCalled = false

        override suspend fun deleteAllCards(): AppResult<Unit> {
            deleteAllCalled = true
            return AppResult.Success(Unit)
        }
    }

    @Test
    fun 모든_카드_삭제를_리포지토리에_위임한다() = runBlocking {
        val repository = RecordingRepository()

        val result = DeleteAllCardsUseCase(repository)()

        assertEquals(true, repository.deleteAllCalled)
        assertEquals(AppResult.Success(Unit), result)
    }
}
