package com.gamss.android.feature.chatSearch

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.paging.PagingData
import com.gamss.android.domain.chattingsearch.ChattingRoomSummary
import com.gamss.android.domain.chattingsearch.SearchChattingRoomsUseCase
import com.gamss.android.domain.repository.ChattingRoomSearchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.orbitmvi.orbit.test.test

/**
 * 검색어가 최소 길이(ChattingRoomSearchPolicy.MIN_KEYWORD_LENGTH) 미만이면
 * INVALID_INPUT 사이드 이펙트를 발행하고 검색 API를 호출하지 않는지 검증한다.
 */
class SearchChattingViewModelTest {

    @Test
    fun `검색어가 2글자 미만이면 검색을 실행하지 않는다`() = runTest {
        val repository = FakeChattingRoomSearchRepository()
        val viewModel = SearchChattingViewModel(SearchChattingRoomsUseCase(repository))
        backgroundScope.launch { viewModel.chattingRooms.collect {} }

        viewModel.test(this) {
            viewModel.onKeywordChanged(TextFieldValue("a"))
            expectState { copy(keyword = TextFieldValue("a")) }

            viewModel.search()
            expectSideEffect(SearchChattingSideEffect.SearchFailure(SearchFailureReason.INVALID_INPUT))
        }

        assertTrue(repository.requestedKeywords.isEmpty())
    }

    @Test
    fun `공백을 제외하면 2글자 미만인 검색어도 검색을 실행하지 않는다`() = runTest {
        val repository = FakeChattingRoomSearchRepository()
        val viewModel = SearchChattingViewModel(SearchChattingRoomsUseCase(repository))
        backgroundScope.launch { viewModel.chattingRooms.collect {} }

        viewModel.test(this) {
            viewModel.onKeywordChanged(TextFieldValue(" a "))
            expectState { copy(keyword = TextFieldValue(" a ")) }

            viewModel.search()
            expectSideEffect(SearchChattingSideEffect.SearchFailure(SearchFailureReason.INVALID_INPUT))
        }

        assertTrue(repository.requestedKeywords.isEmpty())
    }

    @Test
    fun `검색어가 2글자 이상이면 공백을 제거하고 검색을 실행한다`() = runTest {
        val repository = FakeChattingRoomSearchRepository()
        val viewModel = SearchChattingViewModel(SearchChattingRoomsUseCase(repository))
        backgroundScope.launch { viewModel.chattingRooms.collect {} }

        viewModel.test(this) {
            viewModel.onKeywordChanged(TextFieldValue(" ab "))
            expectState { copy(keyword = TextFieldValue(" ab ")) }

            viewModel.search()
            expectState {
                copy(
                    keyword = TextFieldValue(text = "ab", selection = TextRange(2)),
                    hasSearched = true,
                    searchGeneration = 1L,
                )
            }
        }

        assertEquals(listOf("ab"), repository.requestedKeywords)
    }

    private class FakeChattingRoomSearchRepository : ChattingRoomSearchRepository {
        val requestedKeywords = mutableListOf<String>()

        override fun searchChattingRooms(keyword: String): Flow<PagingData<ChattingRoomSummary>> {
            requestedKeywords += keyword
            return flowOf(PagingData.empty())
        }
    }
}
