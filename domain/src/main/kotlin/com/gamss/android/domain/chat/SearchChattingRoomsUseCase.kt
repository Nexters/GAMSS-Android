package com.gamss.android.domain.chat

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.repository.ChattingRoomSearchRepository
import com.gamss.android.domain.usecase.UseCase
import javax.inject.Inject

class SearchChattingRoomsUseCase @Inject constructor(
    private val chattingRoomSearchRepository: ChattingRoomSearchRepository,
) : UseCase<ChattingRoomSearchQuery, AppResult<ChattingRoomSearch>> {

    override suspend fun invoke(
        params: ChattingRoomSearchQuery,
    ): AppResult<ChattingRoomSearch> =
        chattingRoomSearchRepository.searchChattingRooms(params)
}
