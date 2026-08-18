package com.gamss.android.app.main

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.scene.Scene
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
import com.gamss.android.feature.archive.ArchiveDetailScreen
import com.gamss.android.feature.archive.ArchiveScreen
import com.gamss.android.feature.archive.navigation.ArchiveDetailKey
import com.gamss.android.feature.archive.navigation.ArchiveKey
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
    modelDownloadPromptViewModel: ModelDownloadPromptViewModel = hiltViewModel(),
) {
    val destinations = remember { topLevelDestinations() }
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
                // GamssBottomBar 가 navigationBarsPadding()으로 시스템 내비게이션 바를 직접 피하므로,
                // Scaffold 기본 인셋까지 함께 적용하면 이중으로 여백이 생긴다.
                contentWindowInsets = WindowInsets(0),
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
                    // 화면마다 각자 상태바를 피하게 두면 빠뜨리기 쉬우니, 탭 전환 화면들을 모두
                    // 감싸는 이 지점에서 한 번에 처리한다. 배경은 GamssPaperBackground 가 Scaffold
                    // 바깥에서 이미 상태바 뒤까지 이어지므로, 여기서는 콘텐츠만 아래로 민다.
                    modifier = Modifier
                        .padding(innerPadding)
                        .statusBarsPadding(),
                    entries = navigationState.toEntries(entryProvider = entries),
                    // 최상위 탭(홈/보관함/대화) 전환 기본값. 탭은 위계 없는 형제 화면이라
                    // 방향성 있는 슬라이드 대신 fade-through를 쓴다. 상세 화면은 아래 entry의
                    // metadata(detailTransition)가 이 기본값을 덮어쓴다.
                    transitionSpec = tabFadeThroughSpec,
                    popTransitionSpec = tabFadeThroughSpec,
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
    entry<ArchiveKey> {
        ArchiveScreen(
            onNavigateToSetting = { navigator.navigate(SettingKey) },
            onArchiveClick = { navigator.navigate(ArchiveDetailKey(it)) },
        )
    }
    // 보관함 상세도 탭 안쪽의 상세 화면이라 다른 상세들과 같은 슬라이드를 쓴다.
    entry<ArchiveDetailKey>(metadata = detailSlideTransition) { key ->
        ArchiveDetailScreen(
            emotion = key.emotion,
            onBackClick = navigator::goBack,
            onOpenConversation = { conversationId -> navigator.navigate(ChatRoomKey(conversationId)) },
            onNavigateToCardDelete = { navigator.navigate(CardDeleteKey) },
        )
    }
    entry<SettingKey>(metadata = detailSlideTransition) {
        SettingScreen(
            onBackClick = navigator::goBack,
            onAccountInfoClick = { navigator.navigate(AccountInfoKey) },
            onServiceTermsClick = { navigator.navigate(WebViewKey(GamssWebPage.ServiceTerms)) },
            onPrivacyPolicyClick = { navigator.navigate(WebViewKey(GamssWebPage.PrivacyPolicy)) },
        )
    }
    entry<CardDeleteKey>(metadata = detailSlideTransition) {
        CardDeleteScreen(
            onBackClick = navigator::goBack,
            onDeleteComplete = navigator::finishCurrentFlow,
        )
    }
    entry<AccountInfoKey>(metadata = detailSlideTransition) {
        AccountInfoScreen(
            onBackClick = navigator::goBack,
            onNicknameChangeClick = { nickname -> navigator.navigate(NicknameChangeKey(nickname)) },
        )
    }
    entry<NicknameChangeKey>(metadata = detailSlideTransition) { key ->
        NicknameChangeScreen(
            currentNickname = key.currentNickname,
            onBackClick = navigator::goBack,
        )
    }
    entry<WebViewKey>(metadata = detailSlideTransition) { key ->
        WebViewScreen(page = key.page, onBackClick = navigator::goBack)
    }
    entry<ChatKey> {
        ChattingListScreen(
            onChatClick = { navigator.navigate(ChatRoomKey(it)) },
            onMenuClick = { navigator.navigate(SettingKey) },
        )
    }
    entry<ChatRoomKey>(metadata = detailSlideTransition) { key ->
        ChatRoomScreen(
            conversationId = key.conversationId,
            onCardClose = navigator::goBack,
            onBackClick = navigator::goBack,
        )
    }
}

/**
 * 최상위 탭(홈/보관함/대화) 간 전환에 쓰는 기본 트랜지션.
 * 탭은 위계 없는 형제 화면이라 방향성 있는 슬라이드 대신 fade-through를 쓴다.
 */
private val tabFadeThroughSpec: AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform = {
    fadeIn(tween(220, delayMillis = 90)) togetherWith fadeOut(tween(90))
}

/**
 * 탭 내부의 상세 화면(Setting/AccountInfo/NicknameChange/ChatRoom/WebView) push·pop 전용
 * 트랜지션. 계층 이동이라는 방향감을 주기 위해 좌우 슬라이드(shared axis X)를 쓴다.
 *
 * popTransitionSpec과 predictivePopTransitionSpec을 반드시 같이 지정해야 한다 — 인앱 뒤로가기
 * 아이콘(Navigator.goBack() 직접 호출)은 popTransitionSpec을, 폰의 시스템 뒤로가기(제스처·버튼
 * 모두 OnBackInvokedCallback 경유)는 predictivePopTransitionSpec을 따로 참조하기 때문에, 하나만
 * 지정하면 트리거 경로에 따라 모션이 달라져 버린다.
 */
private val detailSlideTransition: Map<String, Any> = NavDisplay.transitionSpec {
    (slideIntoContainer(SlideDirection.Start, tween(300)) + fadeIn(tween(300))) togetherWith
        (slideOutOfContainer(SlideDirection.Start, tween(300)) + fadeOut(tween(150)))
} + NavDisplay.popTransitionSpec {
    (slideIntoContainer(SlideDirection.End, tween(300)) + fadeIn(tween(300))) togetherWith
        (slideOutOfContainer(SlideDirection.End, tween(300)) + fadeOut(tween(150)))
} + NavDisplay.predictivePopTransitionSpec { _ ->
    (slideIntoContainer(SlideDirection.End, tween(300)) + fadeIn(tween(300))) togetherWith
        (slideOutOfContainer(SlideDirection.End, tween(300)) + fadeOut(tween(150)))
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
