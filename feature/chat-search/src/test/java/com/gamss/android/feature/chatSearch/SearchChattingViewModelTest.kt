package com.gamss.android.feature.chatSearch

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.paging.PagingData
import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.conversation.Conversation
import com.gamss.android.domain.conversation.ConversationRepository
import com.gamss.android.domain.conversation.Message
import com.gamss.android.domain.conversation.SentMessage
import com.gamss.android.domain.conversation.chattingsearch.ChattingRoomSummary
import com.gamss.android.domain.conversation.chattingsearch.SearchChattingRoomsUseCase
import com.gamss.android.domain.emotion.EmotionCharacter
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
        val repository = FakeConversationRepository()
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
        val repository = FakeConversationRepository()
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
        val repository = FakeConversationRepository()
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

    private class FakeConversationRepository : ConversationRepository {
        val requestedKeywords = mutableListOf<String>()

        override fun searchChattingRooms(keyword: String): Flow<PagingData<ChattingRoomSummary>> {
            requestedKeywords += keyword
            return flowOf(PagingData.empty())
        }

        override suspend fun getOngoingConversations(): AppResult<List<Conversation>> = unused()

        override suspend fun sendMessage(
            conversationId: Long?,
            content: String,
            replyToMessageId: Long?,
            contextSummary: String?,
            excludeCharacters: Set<EmotionCharacter>,
        ): AppResult<SentMessage> = unused()

        override suspend fun getMessages(conversationId: Long): AppResult<List<Message>> = unused()

        override suspend fun updateTitle(conversationId: Long, title: String): AppResult<Unit> = unused()

        override suspend fun endConversation(conversationId: Long): AppResult<Unit> = unused()

        override suspend fun deleteConversation(conversationId: Long): AppResult<Unit> = unused()

        private fun <T> unused(): T = error("Not used in search ViewModel tests")
    }
}
