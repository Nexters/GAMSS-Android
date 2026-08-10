package com.gamss.android.app.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.gamss.android.app.navigation.Navigator
import com.gamss.android.app.navigation.rememberNavigationState
import com.gamss.android.app.navigation.toEntries
import com.gamss.android.app.navigation.topLevelBottomBarItems
import com.gamss.android.app.navigation.topLevelDestinationKeys
import com.gamss.android.core.ui.GamssBottomBar
import com.gamss.android.feature.calendar.CalendarScreen
import com.gamss.android.feature.calendar.navigation.CalendarKey
import com.gamss.android.feature.chat.ChattingListScreen
import com.gamss.android.feature.chat.navigation.ChatKey
import com.gamss.android.feature.emotion.EmotionScreen
import com.gamss.android.feature.emotion.navigation.EmotionKey
import com.gamss.android.feature.home.HomeScreen
import com.gamss.android.feature.home.navigation.HomeKey
import com.gamss.android.feature.setting.SettingScreen
import com.gamss.android.feature.setting.navigation.SettingKey

@Composable
fun MainScreen() {
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
                    entry<HomeKey> {
                        HomeScreen(onNavigateToSetting = { navigator.navigate(SettingKey) })
                    }
                    entry<ChatKey> { ChattingListScreen() }
                    entry<CalendarKey> { CalendarScreen() }
                    entry<EmotionKey> { EmotionScreen() }
                    entry<SettingKey> { SettingScreen() }
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
