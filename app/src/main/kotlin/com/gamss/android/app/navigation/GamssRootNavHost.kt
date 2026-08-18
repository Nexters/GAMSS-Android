package com.gamss.android.app.navigation

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.gamss.android.app.R
import com.gamss.android.app.main.MainScreen
import com.gamss.android.app.main.MainViewModel
import com.gamss.android.domain.auth.SessionState
import com.gamss.android.feature.login.LoginScreen
import com.gamss.android.feature.login.navigation.LoginKey
import com.gamss.android.feature.onboarding.OnboardingScreen
import com.gamss.android.feature.onboarding.navigation.OnboardingKey
import org.orbitmvi.orbit.compose.collectAsState

@Composable
fun GamssRootNavHost(
    mainViewModel: MainViewModel = hiltViewModel(),
) {
    val state by mainViewModel.collectAsState()

    LifecycleStartEffect(Unit) {
        mainViewModel.syncDeviceToken()
        onStopOrDispose { }
    }

    when (state.sessionState) {
        SessionState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        else -> RootNavDisplay(
            sessionState = state.sessionState,
            useCardFeature = state.useCardFeature,
            shouldPromptNotificationPermission = mainViewModel::shouldPromptNotificationPermission,
            onNotificationPermissionPrompted = mainViewModel::markNotificationPermissionPrompted,
            onNotificationPermissionResult = mainViewModel::syncDeviceToken,
        )
    }
}

@Composable
private fun RootNavDisplay(
    sessionState: SessionState,
    useCardFeature: Boolean,
    shouldPromptNotificationPermission: suspend () -> Boolean,
    onNotificationPermissionPrompted: () -> Unit,
    onNotificationPermissionResult: () -> Unit,
) {
    val initialKey = if (sessionState == SessionState.Authenticated) MainKey else LoginKey
    val backStack = rememberNavBackStack(initialKey)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { onNotificationPermissionResult() },
    )

    // 저장된 안내 이력은 비동기로 반영되므로, 온보딩에서 메인으로 넘어가는 동안에는 이 상태로 중복 요청을 막는다.
    var promptedInSession by remember { mutableStateOf(false) }

    fun markNotificationPermissionPrompted() {
        promptedInSession = true
        onNotificationPermissionPrompted()
    }

    fun requestNotificationPermission() {
        markNotificationPermissionPrompted()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun replaceRoot(destination: NavKey) {
        if (backStack.lastOrNull() != destination) {
            backStack.clear()
            backStack.add(destination)
        }
    }

    LaunchedEffect(sessionState) {
        // 로그인 성공 시에는 LoginResult.isFirstLogin 분기를 기다린다. 세션 상태만 보고 Main으로
        // 보내면 온보딩 콜백과 경쟁하므로, 이 effect는 세션 만료/로그아웃만 처리한다.
        if (sessionState == SessionState.Unauthenticated) {
            replaceRoot(LoginKey)
        }
    }

    NavDisplay(
        entries = backStack.map(
            entryProvider {
                entry<LoginKey> {
                    LoginScreen(
                        googleWebClientId = stringResource(R.string.default_web_client_id),
                        onLoginSuccess = { isFirstLogin ->
                            replaceRoot(if (isFirstLogin) OnboardingKey else MainKey)
                        },
                    )
                }
                entry<OnboardingKey> {
                    OnboardingScreen(
                        onComplete = {
                            // 권한 안내는 동의/거절 모두 온보딩에서 끝난다. 거절도 결정으로 남겨 메인에서 다시 묻지 않는다.
                            markNotificationPermissionPrompted()
                            replaceRoot(MainKey)
                        },
                        onNotificationPermissionRequest = { requestNotificationPermission() },
                    )
                }
                entry<MainKey> {
                    // 온보딩을 거치지 않고 들어오는 기존 사용자에게만 한 번 요청한다.
                    LaunchedEffect(Unit) {
                        if (!promptedInSession && shouldPromptNotificationPermission()) {
                            requestNotificationPermission()
                        }
                    }
                    MainScreen(useCardFeature = useCardFeature)
                }
            },
        ),

        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
        popTransitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
        onBack = {
            if (backStack.size > 1) {
                backStack.removeLastOrNull()
            }
        },
    )
}
