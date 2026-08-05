package com.gamss.android.domain.chattingsearch

import androidx.paging.PagingData
import com.gamss.android.domain.repository.ChattingRoomSearchRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchChattingRoomsUseCase @Inject constructor(
    private val chattingRoomSearchRepository: ChattingRoomSearchRepository,
) {

    operator fun invoke(keyword: String): Flow<PagingData<ChattingRoomSummary>> =
        chattingRoomSearchRepository.searchChattingRooms(keyword)
}
