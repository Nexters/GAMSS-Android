package com.gamss.android.domain.chat

data class ChattingRoomSearchQuery(
    val keyword: String,
    val page: Int = DEFAULT_PAGE,
    val size: Int = DEFAULT_SIZE,
) {
    init {
        require(page >= 0) { "page must be non-negative" }
        require(size > 0) { "size must be positive" }
    }

    companion object {
        const val DEFAULT_PAGE = 0
        const val DEFAULT_SIZE = 20
    }
}
