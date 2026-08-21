package com.gamss.android.feature.chat

import com.gamss.android.domain.repository.TokenUsageAlert

sealed interface ChatRoomSideEffect {
    data class ShowToast(val message: String) : ChatRoomSideEffect
    data class ShowTokenUsageAlert(val alert: TokenUsageAlert) : ChatRoomSideEffect
}
