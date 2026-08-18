package com.gamss.android.domain.chattingsearch

import androidx.paging.PagingData
import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.conversation.Conversation
import com.gamss.android.domain.conversation.ConversationRepository
import com.gamss.android.domain.conversation.Message
import com.gamss.android.domain.conversation.SentMessage
import com.gamss.android.domain.conversation.chattingsearch.ChattingRoomSearchException
import com.gamss.android.domain.conversation.chattingsearch.ChattingRoomSummary
import com.gamss.android.domain.conversation.chattingsearch.SearchChattingRoomsUseCase
import com.gamss.android.domain.emotion.EmotionCharacter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchChattingRoomsUseCaseTest {

    @Test
    fun `최소 길이보다 짧은 검색어는 유효하지 않은 키워드 실패를 반환하고 저장소를 호출하지 않는다`() {
        val repository = FakeConversationRepository()

        val result = SearchChattingRoomsUseCase(repository)("가")

        assertTrue((result as AppResult.Failure).throwable is ChattingRoomSearchException.InvalidKeyword)
        assertEquals(0, repository.searchCallCount)
    }

    @Test
    fun `공백을 제외하면 최소 길이보다 짧은 검색어도 유효하지 않은 키워드 실패를 반환한다`() {
        val repository = FakeConversationRepository()

        val result = SearchChattingRoomsUseCase(repository)(" 가 ")

        assertTrue((result as AppResult.Failure).throwable is ChattingRoomSearchException.InvalidKeyword)
        assertEquals(0, repository.searchCallCount)
    }

    @Test
    fun `유효한 검색어는 앞뒤 공백을 제거해 저장소에 전달한다`() {
        val flow = flowOf(PagingData.empty<ChattingRoomSummary>())
        val repository = FakeConversationRepository(searchResult = flow)

        val result = SearchChattingRoomsUseCase(repository)("  감정  ")

        assertSame(flow, (result as AppResult.Success).data)
        assertEquals("감정", repository.lastKeyword)
        assertEquals(1, repository.searchCallCount)
    }

    private class FakeConversationRepository(
        private val searchResult: Flow<PagingData<ChattingRoomSummary>> = flowOf(PagingData.empty()),
    ) : ConversationRepository {
        var searchCallCount: Int = 0
            private set

        var lastKeyword: String? = null
            private set

        override fun searchChattingRooms(keyword: String): Flow<PagingData<ChattingRoomSummary>> {
            searchCallCount++
            lastKeyword = keyword
            return searchResult
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

        private fun <T> unused(): T = error("Not used in search use case tests")
    }
}
