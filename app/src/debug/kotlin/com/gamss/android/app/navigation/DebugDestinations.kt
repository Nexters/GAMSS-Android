package com.gamss.android.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.gamss.android.feature.chat.ChatRoomScreen
import com.gamss.android.feature.chat.ChattingListScreen
import com.gamss.android.feature.chat.navigation.ChatKey
import com.gamss.android.feature.chat.navigation.ChatRoomKey

/** release 소스셋에 같은 이름의 빈 구현이 있어야 main 이 양쪽에서 컴파일된다. */
val debugTopLevelDestinations = listOf(
    TopLevelDestination(key = ChatKey, icon = Icons.Filled.Email, label = "대화"),
)

fun EntryProviderScope<NavKey>.addDebugEntries(navigator: Navigator) {
    entry<ChatKey> {
        ChattingListScreen(
            onChatClick = { navigator.navigate(ChatRoomKey()) },
        )
    }
    entry<ChatRoomKey> { key ->
        ChatRoomScreen(conversationId = key.conversationId)
    }
}
