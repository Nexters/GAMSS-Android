package com.gamss.android.domain.repository

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.chat.ChattingRoomSearch
import com.gamss.android.domain.chat.ChattingRoomSearchQuery

interface ChattingRoomSearchRepository {

    suspend fun searchChattingRooms(
        query: ChattingRoomSearchQuery,
    ): AppResult<ChattingRoomSearch>
}
