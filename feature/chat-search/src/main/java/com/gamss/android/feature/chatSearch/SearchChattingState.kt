package com.gamss.android.feature.chatSearch

import com.gamss.android.domain.chat.ChattingRoomSummary

data class SearchChattingState(
    val keyword: String = "",
    val rooms: List<ChattingRoomSummary> = emptyList(),
    val isLoading: Boolean = false,
    val isAppending: Boolean = false,
    val hasSearched: Boolean = false,
    val page: Int = 0,
    val canLoadMore: Boolean = false,
) {
    val canSearch: Boolean
        get() = keyword.isNotBlank() && !isLoading && !isAppending
}
