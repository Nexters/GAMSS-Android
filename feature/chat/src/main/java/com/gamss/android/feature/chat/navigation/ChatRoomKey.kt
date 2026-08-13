package com.gamss.android.feature.chat.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** @param initialMessage 새 대화([conversationId] 가 null)일 때만 자동으로 전송한다. */
@Serializable
data class ChatRoomKey(
    val conversationId: Long? = null,
    val initialMessage: String? = null,
) : NavKey
