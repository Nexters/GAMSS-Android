package com.gamss.android.domain.repository

import androidx.paging.PagingData
import com.gamss.android.domain.chattingsearch.ChattingRoomSummary
import kotlinx.coroutines.flow.Flow

interface ChattingRoomSearchRepository {

    fun searchChattingRooms(keyword: String): Flow<PagingData<ChattingRoomSummary>>
}
