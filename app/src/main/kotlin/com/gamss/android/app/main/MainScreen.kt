package com.gamss.android.app.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.gamss.android.app.navigation.Navigator
import com.gamss.android.app.navigation.bottomBarItems
import com.gamss.android.app.navigation.keys
import com.gamss.android.app.navigation.rememberNavigationState
import com.gamss.android.app.navigation.toEntries
import com.gamss.android.app.navigation.topLevelDestinations
import com.gamss.android.core.designsystem.component.GamssBottomBar
import com.gamss.android.core.designsystem.component.GamssPaperBackground
import com.gamss.android.core.designsystem.theme.GamssTheme
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
    val entries = remember(navigator) { mainEntryProvider(navigator) }

    // 배경/탭바 에셋이 라이트 전용이라 다크 시안이 나올 때까지 셸은 라이트로 고정한다.
    GamssTheme(darkTheme = false) {
        // 종이 보드가 상태바와 탭바 뒤까지 이어져야 해서 Scaffold 바깥에 깐다.
        GamssPaperBackground {
            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = {
                    if (navigationState.currentKey == navigationState.currentTopLevelKey) {
                        GamssBottomBar(
                            items = destinations.bottomBarItems(),
                            selectedValue = navigationState.currentTopLevelKey,
                            onItemClick = navigator::navigate,
                        )
                    }
                },
            ) { innerPadding ->
                NavDisplay(
                    modifier = Modifier.padding(innerPadding),
                    entries = navigationState.toEntries(entryProvider = entries),
                    onBack = {
                        if (navigationState.canGoBack) {
                            navigator.goBack()
                        }
                    },
                )
            }
        }
    }
}

private fun mainEntryProvider(navigator: Navigator) = entryProvider {
    entry<HomeKey> {
        HomeScreen(
            onNavigateToSetting = { navigator.navigate(SettingKey) },
            onStartConversation = { message -> navigator.navigate(ChatRoomKey(initialMessage = message)) },
        )
    }
    entry<CalendarKey> { CalendarScreen() }
    entry<EmotionKey> { EmotionScreen() }
    entry<SettingKey> {
        SettingScreen(
            onBackClick = navigator::goBack,
            onAccountInfoClick = { navigator.navigate(AccountInfoKey) },
            onServiceTermsClick = { navigator.navigate(ServiceTermsKey) },
            onPrivacyPolicyClick = { navigator.navigate(PrivacyPolicyKey) },
        )
    }
    entry<AccountInfoKey> {
        AccountInfoScreen(
            onBackClick = navigator::goBack,
            onNicknameChangeClick = { nickname -> navigator.navigate(NicknameChangeKey(nickname)) },
        )
    }
    entry<NicknameChangeKey> { key ->
        NicknameChangeScreen(
            currentNickname = key.currentNickname,
            onBackClick = navigator::goBack,
        )
    }
    entry<ServiceTermsKey> { ServiceTermsScreen(onBackClick = navigator::goBack) }
    entry<PrivacyPolicyKey> { PrivacyPolicyScreen(onBackClick = navigator::goBack) }
    entry<ChatKey> {
        ChattingListScreen(onChatClick = { navigator.navigate(ChatRoomKey(conversationId = it)) })
    }
    entry<ChatRoomKey> { key ->
        ChatRoomScreen(
            conversationId = key.conversationId,
            initialMessage = key.initialMessage,
            onCardClose = navigator::goBack,
        )
    }
}
