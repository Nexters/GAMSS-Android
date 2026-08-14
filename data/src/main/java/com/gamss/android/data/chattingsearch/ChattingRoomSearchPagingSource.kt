package com.gamss.android.data.chattingsearch

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.gamss.android.core.common.AppResult
import com.gamss.android.data.remote.conversation.ConversationService
import com.gamss.android.data.remote.conversation.model.response.toDomain
import com.gamss.android.data.remote.runCatchingApiCall
import com.gamss.android.domain.conversation.chattingsearch.ChattingRoomSummary

internal class ChattingRoomSearchPagingSource(
    private val conversationService: ConversationService,
    private val keyword: String,
) : PagingSource<Int, ChattingRoomSummary>() {

    private val seenIds = mutableSetOf<Long>()

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ChattingRoomSummary> {
        val page = params.key ?: DEFAULT_PAGE
        val result = runCatchingApiCall {
            checkNotNull(
                conversationService.searchChattingRooms(
                    keyword = keyword,
                    page = page,
                    size = params.loadSize,
                ).data,
            ) { "No available chatting room search data" }.toDomain()
        }

        return when (result) {
            is AppResult.Success -> {
                val uniqueRooms = result.data.rooms.filter { room ->
                    seenIds.add(room.conversationId)
                }
                LoadResult.Page(
                    data = uniqueRooms,
                    prevKey = page.takeIf { it > DEFAULT_PAGE }?.minus(1),
                    nextKey = (page + 1).takeIf { result.data.hasNextPage },
                )
            }

            is AppResult.Failure -> LoadResult.Error(result.throwable)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, ChattingRoomSummary>): Int? =
        state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.let { page ->
                page.prevKey?.plus(1) ?: page.nextKey?.minus(1)
            }
        }

    companion object {
        const val DEFAULT_PAGE = 0
        const val DEFAULT_SIZE = 20
    }
}
