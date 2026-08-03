package com.gamss.android.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.gamss.android.feature.chat.ChatRoomScreen
import com.gamss.android.feature.chat.ChattingListScreen
import com.gamss.android.feature.chat.navigation.ChatKey
import com.gamss.android.feature.chat.navigation.ChatRoomKey

/**
 * debug 빌드에서만 진입할 수 있는 화면. release 소스셋에는 같은 이름의 빈 구현이 있고,
 * :feature:chat 은 debugImplementation 이라 release 에는 아예 포함되지 않는다.
 */
val debugTopLevelDestinations = listOf(
    TopLevelDestination(key = ChatKey, icon = Icons.Filled.Email, label = "대화"),
)

fun EntryProviderScope<NavKey>.addDebugEntries(navigator: Navigator) {
    entry<ChatKey> {
        ChattingListScreen(
            // 목록이 아직 더미라 실제 채팅방 ID 가 없다. 서버가 첫 전송에서 만든다.
            onChatClick = { navigator.navigate(ChatRoomKey()) },
        )
    }
    entry<ChatRoomKey> { key ->
        ChatRoomScreen(conversationId = key.conversationId)
    }
}
