package com.gamss.android.feature.chatSearch

import androidx.compose.ui.text.input.TextFieldValue

data class SearchChattingState(
    val keyword: TextFieldValue = TextFieldValue(),
    val hasSearched: Boolean = false,
    val searchGeneration: Long = 0L,
)
