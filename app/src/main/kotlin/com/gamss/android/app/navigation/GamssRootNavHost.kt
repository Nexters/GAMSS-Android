package com.gamss.android.app.navigation

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
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
        )
    }
}

@Composable
private fun RootNavDisplay(
    sessionState: SessionState,
    useCardFeature: Boolean,
) {
    val initialKey = if (sessionState == SessionState.Authenticated) MainKey else LoginKey
    val backStack = rememberNavBackStack(initialKey)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {},
    )

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
                        onComplete = { replaceRoot(MainKey) },
                        onBackClick = {},
                        onNotificationPermissionRequest = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                    )
                }
                entry<MainKey> { MainScreen(useCardFeature = useCardFeature) }
            },
        ),
        onBack = {
            if (backStack.size > 1) {
                backStack.removeLastOrNull()
            }
        },
    )
}
