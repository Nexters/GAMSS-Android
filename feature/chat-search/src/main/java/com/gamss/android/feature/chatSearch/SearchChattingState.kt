package com.gamss.android.feature.chatSearch

data class SearchChattingState(
    val keyword: String = "",
    val hasSearched: Boolean = false,
    val searchGeneration: Long = 0L,
) {
    val canSearch: Boolean
        get() = keyword.isNotBlank()
}
