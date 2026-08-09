package com.gamss.android.feature.chatSearch

sealed interface SearchChattingSideEffect {
    data class SearchFailure(val reason: SearchFailureReason) : SearchChattingSideEffect
}

enum class SearchFailureReason {
    INVALID_INPUT,
    NETWORK,
    UNKNOWN,
}
