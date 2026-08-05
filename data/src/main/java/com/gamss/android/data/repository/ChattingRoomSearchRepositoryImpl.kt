package com.gamss.android.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.gamss.android.data.ChattingSearchPagingSource
import com.gamss.android.data.ChattingSearchPagingSource.Companion.DEFAULT_SIZE
import com.gamss.android.data.remote.chattingRoomSearch.ChattingRoomSearchService
import com.gamss.android.domain.chattingsearch.ChattingRoomSummary
import com.gamss.android.domain.repository.ChattingRoomSearchRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ChattingRoomSearchRepositoryImpl @Inject constructor(
    private val chattingRoomSearchService: ChattingRoomSearchService,
) : ChattingRoomSearchRepository {

    override fun searchChattingRooms(keyword: String): Flow<PagingData<ChattingRoomSummary>> =
        Pager(
            config = PagingConfig(
                pageSize = DEFAULT_SIZE,
                initialLoadSize = DEFAULT_SIZE,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = {
                ChattingSearchPagingSource(
                    chattingRoomSearchService = chattingRoomSearchService,
                    keyword = keyword,
                )
            },
        ).flow
}
