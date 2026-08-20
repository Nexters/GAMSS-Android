package com.gamss.android.domain.card

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ClearCardCacheUseCaseTest {

    private class RecordingRepository : FakeCardRepository() {
        var clearCacheCalled = false

        override suspend fun clearCache() {
            clearCacheCalled = true
        }
    }

    @Test
    fun 캐시_비우기를_리포지토리에_위임한다() = runBlocking {
        val repository = RecordingRepository()

        ClearCardCacheUseCase(repository)()

        assertEquals(true, repository.clearCacheCalled)
    }
}
