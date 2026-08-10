package com.gamss.android.feature.chat

sealed interface ChatRoomSideEffect {
    data class ShowToast(val message: String) : ChatRoomSideEffect
}
