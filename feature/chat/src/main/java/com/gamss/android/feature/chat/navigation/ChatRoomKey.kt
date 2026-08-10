package com.gamss.android.feature.chat.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class ChatRoomKey(val conversationId: Long? = null) : NavKey
