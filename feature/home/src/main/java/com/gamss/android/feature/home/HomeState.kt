package com.gamss.android.feature.home

data class HomeState(
    val isLoading: Boolean = false,
    val greeting: String = "",
    val isTokenUsageLoading: Boolean = false,
    val tokenUsageDisplayMode: TokenUsageDisplayMode = TokenUsageDisplayMode.PERCENT,
    val tokenUsage: TokenUsageUiModel? = null,
)
