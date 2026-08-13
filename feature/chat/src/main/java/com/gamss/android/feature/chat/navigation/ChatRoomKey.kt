package com.gamss.android.feature.chat.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** 이미 만들어진 대화를 연다. 대화 생성은 여는 쪽이 끝내 두고 id 만 넘긴다. */
@Serializable
data class ChatRoomKey(val conversationId: Long) : NavKey
