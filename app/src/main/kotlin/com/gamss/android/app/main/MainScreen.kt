package com.gamss.android.app.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.gamss.android.feature.setting.navigation.PrivacyPolicyKey
import com.gamss.android.feature.setting.navigation.ServiceTermsKey
import com.gamss.android.feature.setting.navigation.SettingKey
import com.gamss.android.feature.setting.nicknamechange.NicknameChangeScreen
import com.gamss.android.feature.setting.privacypolicy.PrivacyPolicyScreen
import com.gamss.android.feature.setting.serviceterms.ServiceTermsScreen

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
                            onServiceTermsClick = { navigator.navigate(ServiceTermsKey) },
                            onPrivacyPolicyClick = { navigator.navigate(PrivacyPolicyKey) },
                            context = LocalContext.current
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
                    entry<ServiceTermsKey> {
                        ServiceTermsScreen(onBackClick = navigator::goBack)
                    }
                    entry<PrivacyPolicyKey> {
                        PrivacyPolicyScreen(onBackClick = navigator::goBack)
                    }
                    if (isDebug) {
                        entry<ChatKey> {
                            ChattingListScreen(onChatClick = { navigator.navigate(ChatRoomKey(it)) })
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
