package com.gamss.android.data.repository

import com.gamss.android.core.common.AppResult
import com.gamss.android.data.remote.chattingRoomSearch.ChattingRoomSearchService
import com.gamss.android.data.remote.chattingRoomSearch.model.response.toDomain
import com.gamss.android.domain.chat.ChattingRoomSearch
import com.gamss.android.domain.chat.ChattingRoomSearchQuery
import com.gamss.android.domain.repository.ChattingRoomSearchRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ChattingRoomSearchRepositoryImpl @Inject constructor(
    private val chattingRoomSearchService: ChattingRoomSearchService,
) : ChattingRoomSearchRepository {

    override suspend fun searchChattingRooms(
        query: ChattingRoomSearchQuery,
    ): AppResult<ChattingRoomSearch> {
        return runCatchingApiCall {
            val response = chattingRoomSearchService.searchChattingRooms(
                keyword = query.keyword,
                page = query.page,
                size = query.size,
            )
            checkNotNull(response.data) { "No available chatting room search data" }.toDomain()
        }
    }
}
