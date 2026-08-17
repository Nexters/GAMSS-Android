package com.gamss.android.feature.chat

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.orbitmvi.orbit.test.test

@OptIn(ExperimentalCoroutinesApi::class)
class ChattingListSearchTest {

    @Test
    fun `검색어가 2글자 미만이면 검색을 실행하지 않는다`() = runTest {
        val repository = FakeChattingListRepository()
        val viewModel = chattingListViewModel(repository)
        backgroundScope.launch { viewModel.chattingRooms.collect {} }

        viewModel.test(this) {
            viewModel.onSearchKeywordChanged(TextFieldValue("a"))
            expectState { copy(search = search.copy(keyword = TextFieldValue("a"))) }

            viewModel.search()
            expectSideEffect(
                ChattingListSideEffect.ShowSearchFailed(SearchFailureReason.INVALID_INPUT),
            )
        }

        assertTrue(repository.requestedKeywords.isEmpty())
    }

    @Test
    fun `공백을 제외하면 2글자 미만인 검색어도 검색을 실행하지 않는다`() = runTest {
        val repository = FakeChattingListRepository()
        val viewModel = chattingListViewModel(repository)
        backgroundScope.launch { viewModel.chattingRooms.collect {} }

        viewModel.test(this) {
            viewModel.onSearchKeywordChanged(TextFieldValue(" a "))
            expectState { copy(search = search.copy(keyword = TextFieldValue(" a "))) }

            viewModel.search()
            expectSideEffect(
                ChattingListSideEffect.ShowSearchFailed(SearchFailureReason.INVALID_INPUT),
            )
        }

        assertTrue(repository.requestedKeywords.isEmpty())
    }

    @Test
    fun `검색어가 2글자 이상이면 입력 상태를 유지하고 공백을 제거해 검색한다`() = runTest {
        val repository = FakeChattingListRepository()
        val viewModel = chattingListViewModel(repository)
        val input = TextFieldValue(text = " ab ", selection = TextRange(2))
        backgroundScope.launch { viewModel.chattingRooms.collect {} }

        viewModel.test(this) {
            viewModel.onSearchKeywordChanged(input)
            expectState { copy(search = search.copy(keyword = input)) }

            viewModel.search()
            expectState {
                copy(
                    search = search.copy(
                        keyword = input,
                        hasSearched = true,
                        searchGeneration = 1L,
                    ),
                )
            }
        }

        assertEquals(listOf("ab"), repository.requestedKeywords)
    }

    @Test
    fun `검색을 취소하면 검색 상태를 초기화한다`() = runTest {
        val repository = FakeChattingListRepository()
        val viewModel = chattingListViewModel(repository)

        viewModel.test(this) {
            containerHost.load()
            runCurrent()
            containerHost.onSearchModeEnter()
            containerHost.onSearchKeywordChanged(TextFieldValue("검색어"))
            runCurrent()
            assertTrue(containerHost.container.stateFlow.value.search.isActive)

            containerHost.onSearchCancel()
            runCurrent()
            assertEquals(ChattingSearchState(), containerHost.container.stateFlow.value.search)
            cancelAndIgnoreRemainingItems()
        }

        assertFalse(viewModel.container.stateFlow.value.search.isActive)
    }

    @Test
    fun `검색 결과를 선택해도 검색 상태를 유지한다`() = runTest {
        val repository = FakeChattingListRepository(listOf(conversation(1L)))
        val viewModel = chattingListViewModel(repository)
        backgroundScope.launch { viewModel.chattingRooms.collect {} }

        viewModel.test(this) {
            containerHost.load()
            containerHost.onSearchModeEnter()
            containerHost.onSearchKeywordChanged(TextFieldValue("검색어"))
            containerHost.search()
            runCurrent()

            containerHost.onCardLongClick(1L)
            runCurrent()

            assertEquals(ChattingListPhase.Selecting(setOf(1L)), containerHost.phase())
            assertTrue(containerHost.container.stateFlow.value.search.isActive)
            assertTrue(containerHost.container.stateFlow.value.search.hasSearched)
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `검색 결과를 삭제하면 같은 검색어로 결과를 갱신한다`() = runTest {
        val repository = FakeChattingListRepository(listOf(conversation(1L)))
        val viewModel = chattingListViewModel(repository)
        backgroundScope.launch { viewModel.chattingRooms.collect {} }

        viewModel.test(this) {
            containerHost.load()
            containerHost.onSearchModeEnter()
            containerHost.onSearchKeywordChanged(TextFieldValue("검색어"))
            containerHost.search()
            containerHost.onCardLongClick(1L)
            containerHost.onDeleteRequest()
            containerHost.onDeleteConfirm()
            runCurrent()

            assertEquals(listOf("검색어", "검색어"), repository.requestedKeywords)
            assertTrue(containerHost.container.stateFlow.value.search.isActive)
            cancelAndIgnoreRemainingItems()
        }
    }
}
