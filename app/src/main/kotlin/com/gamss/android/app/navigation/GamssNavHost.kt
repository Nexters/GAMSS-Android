package com.gamss.android.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.gamss.android.core.ui.GamssBottomBar
import com.gamss.android.feature.calendar.CalendarScreen
import com.gamss.android.feature.calendar.navigation.CalendarKey
import com.gamss.android.feature.chat.ChatRoomScreen
import com.gamss.android.feature.chat.ChattingListScreen
import com.gamss.android.feature.chat.navigation.ChatKey
import com.gamss.android.feature.chat.navigation.ChatRoomKey
import com.gamss.android.feature.emotion.EmotionScreen
import com.gamss.android.feature.emotion.navigation.EmotionKey
import com.gamss.android.feature.home.HomeScreen
import com.gamss.android.feature.home.navigation.HomeKey

@Composable
fun GamssNavHost() {
    val navigationState = rememberNavigationState(
        startKey = HomeKey,
        topLevelKeys = topLevelDestinationKeys,
    )
    val navigator = remember(navigationState) { Navigator(navigationState) }

    Scaffold(
        bottomBar = {
            if (navigationState.currentKey == navigationState.currentTopLevelKey) {
                GamssBottomBar(
                    items = topLevelBottomBarItems,
                    selectedValue = navigationState.currentTopLevelKey,
                    onItemClick = navigator::navigate,
                )
            }
        },
    ) { innerPadding ->
        NavDisplay(
            modifier = Modifier.padding(innerPadding),
            entries = navigationState.toEntries(
                entryProvider = entryProvider {
                    entry<HomeKey> { HomeScreen() }
                    entry<ChatKey> {
                        ChattingListScreen(
                            // 목록이 아직 더미라 실제 채팅방 ID 가 없다. 서버가 첫 전송에서 만든다.
                            onChatClick = { navigator.navigate(ChatRoomKey()) },
                        )
                    }
                    entry<ChatRoomKey> { key ->
                        ChatRoomScreen(
                            conversationId = key.conversationId,
                            onCardClose = navigator::goBack,
                        )
                    }
                    entry<CalendarKey> { CalendarScreen() }
                    entry<EmotionKey> { EmotionScreen() }
                },
            ),
            onBack = {
                if (navigationState.canGoBack) {
                    navigator.goBack()
                }
            },
        )
    }
}
