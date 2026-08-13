package com.gamss.android.feature.chat

sealed interface ChattingListSideEffect {
    data class ShowToast(val message: String) : ChattingListSideEffect
}
