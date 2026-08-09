package com.gamss.android.domain.chattingsearch

import androidx.paging.PagingData
import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.repository.ChattingRoomSearchRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchChattingRoomsUseCase @Inject constructor(
    private val chattingRoomSearchRepository: ChattingRoomSearchRepository,
) {

    operator fun invoke(keyword: String): AppResult<Flow<PagingData<ChattingRoomSummary>>> {
        val trimmedKeyword = keyword.trim()

        validate(trimmedKeyword)?.let { return AppResult.Failure(it) }

        return AppResult.Success(chattingRoomSearchRepository.searchChattingRooms(trimmedKeyword))
    }

    private fun validate(keyword: String): ChattingRoomSearchException? =
        when {
            keyword.length < ChattingRoomSearchPolicy.MIN_KEYWORD_LENGTH -> ChattingRoomSearchException.InvalidKeyword()
            else -> null
        }
}
