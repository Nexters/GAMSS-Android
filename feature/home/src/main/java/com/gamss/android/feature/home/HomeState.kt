package com.gamss.android.feature.home

data class HomeState(
    val isLoading: Boolean = true,
    val nickname: String? = null,
    val input: String = "",
)
