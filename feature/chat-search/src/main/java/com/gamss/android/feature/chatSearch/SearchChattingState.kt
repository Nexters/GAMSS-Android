package com.gamss.android.feature.chatSearch

import androidx.compose.ui.text.input.TextFieldValue

internal const val MIN_SEARCH_KEYWORD_LENGTH = 2

data class SearchChattingState(
    val keyword: TextFieldValue = TextFieldValue(),
    val hasSearched: Boolean = false,
    val searchGeneration: Long = 0L,
) {
    val canSearch: Boolean
        get() = keyword.text.trim().length >= MIN_SEARCH_KEYWORD_LENGTH
}
