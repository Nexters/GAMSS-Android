package com.gamss.android.feature.chatSearch

sealed interface SearchChattingSideEffect {
    data class ShowToast(val message: String) : SearchChattingSideEffect
}
