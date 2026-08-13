package com.gamss.android.app.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.gamss.android.app.navigation.Navigator
import com.gamss.android.app.navigation.bottomBarItems
import com.gamss.android.app.navigation.keys
import com.gamss.android.app.navigation.rememberNavigationState
import com.gamss.android.app.navigation.toEntries
import com.gamss.android.app.navigation.topLevelDestinations
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
import com.gamss.android.feature.setting.accountinfo.AccountInfoScreen
import com.gamss.android.feature.setting.main.SettingScreen
import com.gamss.android.feature.setting.navigation.AccountInfoKey
import com.gamss.android.feature.setting.navigation.NicknameChangeKey
import com.gamss.android.feature.setting.navigation.SettingKey
import com.gamss.android.feature.setting.nicknamechange.NicknameChangeScreen
import com.gamss.android.feature.webview.GamssWebPage
import com.gamss.android.feature.webview.WebViewScreen
import com.gamss.android.feature.webview.navigation.WebViewKey

@Composable
fun MainScreen(isDebug: Boolean) {
    val destinations = remember(isDebug) { topLevelDestinations(isDebug) }
    val navigationState = rememberNavigationState(
        startKey = HomeKey,
        topLevelKeys = remember(destinations) { destinations.keys() },
    )
    val navigator = remember(navigationState) { Navigator(navigationState) }

    Scaffold(
        bottomBar = {
            if (navigationState.currentKey == navigationState.currentTopLevelKey) {
                GamssBottomBar(
                    items = remember(destinations) { destinations.bottomBarItems() },
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
                    entry<CalendarKey> { CalendarScreen() }
                    entry<EmotionKey> { EmotionScreen() }
                    entry<SettingKey> {
                        SettingScreen(
                            onBackClick = navigator::goBack,
                            onAccountInfoClick = { navigator.navigate(AccountInfoKey) },
                            onServiceTermsClick = { navigator.navigate(WebViewKey(GamssWebPage.ServiceTerms)) },
                            onPrivacyPolicyClick = { navigator.navigate(WebViewKey(GamssWebPage.PrivacyPolicy)) },
                        )
                    }
                    entry<AccountInfoKey> {
                        AccountInfoScreen(
                            onBackClick = navigator::goBack,
                            onNicknameChangeClick = { nickname ->
                                navigator.navigate(NicknameChangeKey(nickname))
                            },
                        )
                    }
                    entry<NicknameChangeKey> { key ->
                        NicknameChangeScreen(
                            currentNickname = key.currentNickname,
                            onBackClick = navigator::goBack,
                        )
                    }
                    entry<WebViewKey> { key ->
                        WebViewScreen(page = key.page, onBackClick = navigator::goBack)
                    }
                    // 대화방 화면에 디자인이 적용되고 탭 아이콘이 확정되면 게이트를 해제한다.
                    if (isDebug) {
                        entry<ChatKey> {
                            ChattingListScreen(
                                onChatClick = { navigator.navigate(ChatRoomKey(it)) },
                                onMenuClick = { navigator.navigate(SettingKey) },
                            )
                        }
                        entry<ChatRoomKey> { key ->
                            ChatRoomScreen(
                                conversationId = key.conversationId,
                                onCardClose = navigator::goBack,
                            )
                        }
                    }
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
