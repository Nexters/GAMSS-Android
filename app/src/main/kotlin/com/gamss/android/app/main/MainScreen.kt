package com.gamss.android.app.main

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.gamss.android.app.navigation.Navigator
import com.gamss.android.app.navigation.bottomBarItems
import com.gamss.android.app.navigation.keys
import com.gamss.android.app.navigation.rememberNavigationState
import com.gamss.android.app.navigation.toEntries
import com.gamss.android.app.navigation.topLevelDestinations
import com.gamss.android.app.navigation.visibleIn
import com.gamss.android.core.designsystem.component.GamssBottomBar
import com.gamss.android.core.designsystem.component.GamssPaperBackground
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.feature.calendar.CalendarScreen
import com.gamss.android.feature.calendar.navigation.CalendarKey
import com.gamss.android.feature.carddelete.CardDeleteScreen
import com.gamss.android.feature.carddelete.navigation.CardDeleteKey
import com.gamss.android.feature.chat.ChatRoomScreen
import com.gamss.android.feature.chat.ChattingListScreen
import com.gamss.android.feature.chat.navigation.ChatKey
import com.gamss.android.feature.chat.navigation.ChatRoomKey
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
fun MainScreen(
    useCardFeature: Boolean,
    modelDownloadPromptViewModel: ModelDownloadPromptViewModel = hiltViewModel(),
) {
    val destinations = remember { topLevelDestinations() }
    val visibleDestinations = remember(destinations, useCardFeature) { destinations.visibleIn(useCardFeature) }
    val navigationState = rememberNavigationState(
        startKey = HomeKey,
        topLevelKeys = remember(destinations) { destinations.keys() },
    )
    val navigator = remember(navigationState) { Navigator(navigationState) }
    val entries = remember(navigator) { mainEntryProvider(navigator) }
    val snackbarHostState = remember { SnackbarHostState() }

    ModelDownloadConfirmationEffect(modelDownloadPromptViewModel, snackbarHostState)

    // 배경/탭바 에셋이 라이트 전용이라 다크 시안이 나올 때까지 셸은 라이트로 고정한다.
    GamssTheme(darkTheme = false) {
        // 종이 보드가 상태바와 탭바 뒤까지 이어져야 해서 Scaffold 바깥에 깐다.
        GamssPaperBackground {
            Scaffold(
                containerColor = Color.Transparent,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (navigationState.currentKey == navigationState.currentTopLevelKey) {
                        GamssBottomBar(
                            items = visibleDestinations.bottomBarItems(),
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
            onOpenConversation = { conversationId -> navigator.navigate(ChatRoomKey(conversationId)) },
        )
    }
    entry<CalendarKey> { CalendarScreen() }
    entry<SettingKey> {
        SettingScreen(
            onBackClick = navigator::goBack,
            onAccountInfoClick = { navigator.navigate(AccountInfoKey) },
            onServiceTermsClick = { navigator.navigate(WebViewKey(GamssWebPage.ServiceTerms)) },
            onPrivacyPolicyClick = { navigator.navigate(WebViewKey(GamssWebPage.PrivacyPolicy)) },
        )
    }
    entry<CardDeleteKey> {
        CardDeleteScreen(
            onBackClick = navigator::goBack,
            onDeleteComplete = navigator::finishCurrentFlow,
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
    entry<WebViewKey> { key ->
        WebViewScreen(page = key.page, onBackClick = navigator::goBack)
    }
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

/**
 * emotion/summary 온디바이스 모델 중 하나라도 셀룰러/크기 확인이 필요해지면 스낵바를 띄운다.
 * 상태 기반이라 한 세션에서 여러 번(예: emotion 이 먼저, summary 가 나중에) 뜰 수 있다 —
 * [needsUserConfirmation] 가 다시 true 가 될 때마다 재노출된다.
 */
@Composable
private fun ModelDownloadConfirmationEffect(
    viewModel: ModelDownloadPromptViewModel,
    snackbarHostState: SnackbarHostState,
) {
    val activity = LocalActivity.current
    val needsConfirmation by viewModel.needsUserConfirmation.collectAsState()

    LaunchedEffect(needsConfirmation, activity) {
        if (!needsConfirmation || activity == null) return@LaunchedEffect

        val result = snackbarHostState.showSnackbar(
            message = MODEL_DOWNLOAD_MESSAGE,
            actionLabel = MODEL_DOWNLOAD_ACTION,
            withDismissAction = true,
        )
        if (result == SnackbarResult.ActionPerformed) {
            viewModel.onConfirmDownload(activity)
        }
    }
}

private const val MODEL_DOWNLOAD_MESSAGE = "추가 다운로드가 필요해요"
private const val MODEL_DOWNLOAD_ACTION = "모바일 데이터로 받기"
