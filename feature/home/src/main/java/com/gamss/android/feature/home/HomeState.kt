package com.gamss.android.feature.home

data class HomeState(
    val isLoading: Boolean = false,
    val greeting: String = "",
    val isTokenUsageLoading: Boolean = false,
    val tokenUsage: TokenUsageUiModel? = null,
)
