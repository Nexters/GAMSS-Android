package com.gamss.android.feature.chat.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * 한계: 목록이 더미라 항상 null 로 열린다. 첫 전송으로 받은 채팅방 ID 는 화면 state 에만 있어
 * 뒤로 나가면 다음 진입은 새 채팅방이 된다. 목록 조회 API 를 붙여 실제 ID 를 넘기면 해결된다.
 */
@Serializable
data class ChatRoomKey(val conversationId: Long? = null) : NavKey
