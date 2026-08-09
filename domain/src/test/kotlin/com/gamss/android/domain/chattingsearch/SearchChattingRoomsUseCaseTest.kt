package com.gamss.android.domain.chattingsearch

import androidx.paging.PagingData
import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.repository.ChattingRoomSearchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchChattingRoomsUseCaseTest {

    @Test
    fun `최소 길이보다 짧은 검색어는 유효하지 않은 키워드 실패를 반환하고 저장소를 호출하지 않는다`() {
        val repository = FakeChattingRoomSearchRepository()

        val result = SearchChattingRoomsUseCase(repository)("가")

        assertTrue((result as AppResult.Failure).throwable is ChattingRoomSearchException.InvalidKeyword)
        assertEquals(0, repository.searchCallCount)
    }

    @Test
    fun `공백을 제외하면 최소 길이보다 짧은 검색어도 유효하지 않은 키워드 실패를 반환한다`() {
        val repository = FakeChattingRoomSearchRepository()

        val result = SearchChattingRoomsUseCase(repository)(" 가 ")

        assertTrue((result as AppResult.Failure).throwable is ChattingRoomSearchException.InvalidKeyword)
        assertEquals(0, repository.searchCallCount)
    }

    @Test
    fun `유효한 검색어는 앞뒤 공백을 제거해 저장소에 전달한다`() {
        val flow = flowOf(PagingData.empty<ChattingRoomSummary>())
        val repository = FakeChattingRoomSearchRepository(searchResult = flow)

        val result = SearchChattingRoomsUseCase(repository)("  감정  ")

        assertSame(flow, (result as AppResult.Success).data)
        assertEquals("감정", repository.lastKeyword)
        assertEquals(1, repository.searchCallCount)
    }

    private class FakeChattingRoomSearchRepository(
        private val searchResult: Flow<PagingData<ChattingRoomSummary>> = flowOf(PagingData.empty()),
    ) : ChattingRoomSearchRepository {
        var searchCallCount: Int = 0
            private set

        var lastKeyword: String? = null
            private set

        override fun searchChattingRooms(keyword: String): Flow<PagingData<ChattingRoomSummary>> {
            searchCallCount++
            lastKeyword = keyword
            return searchResult
        }
    }
}
