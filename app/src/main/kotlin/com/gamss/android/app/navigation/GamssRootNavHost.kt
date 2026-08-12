package com.gamss.android.app.navigation

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
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.gamss.android.app.R
import com.gamss.android.app.main.MainScreen
import com.gamss.android.app.main.MainViewModel
import com.gamss.android.domain.auth.SessionState
import com.gamss.android.feature.login.LoginScreen
import com.gamss.android.feature.login.navigation.LoginKey
import org.orbitmvi.orbit.compose.collectAsState

/**
 * 앱의 최상위 진입 지점.
 *
 * 앱 시작 시 저장된 세션을 확인하고, 로그인 이전 흐름과
 * Bottom Navigation 기반 메인 영역 사이의 root back stack을 관리한다.
 */
@Composable
fun GamssRootNavHost(
    isDebug: Boolean,
    mainViewModel: MainViewModel = hiltViewModel(),
) {
    val sessionState by mainViewModel.collectAsState()

    when (sessionState) {
        SessionState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        else -> RootNavDisplay(
            sessionState = sessionState,
            isDebug = isDebug,
        )
    }
}

@Composable
private fun RootNavDisplay(
    sessionState: SessionState,
    isDebug: Boolean,
) {
    val initialKey = if (sessionState == SessionState.Authenticated) MainKey else LoginKey
    val backStack = rememberNavBackStack(initialKey)

    LaunchedEffect(sessionState) {
        val destination = when (sessionState) {
            SessionState.Authenticated -> MainKey
            SessionState.Unauthenticated -> LoginKey
            SessionState.Loading -> return@LaunchedEffect
        }
        if (backStack.lastOrNull() != destination) {
            backStack.clear()
            backStack.add(destination)
        }
    }

    NavDisplay(
        entries = backStack.map(
            entryProvider {
                entry<LoginKey> {
                    LoginScreen(
                        googleWebClientId = stringResource(R.string.default_web_client_id),
                    )
                }
                entry<MainKey> { MainScreen(isDebug = isDebug) }
            },
        ),
        onBack = {
            if (backStack.size > 1) {
                backStack.removeLastOrNull()
            }
        },
    )
}
