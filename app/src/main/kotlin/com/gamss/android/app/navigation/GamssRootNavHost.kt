package com.gamss.android.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.gamss.android.feature.login.LoginKey
import com.gamss.android.feature.login.LoginScreen

/**
 * 앱의 최상위 진입 지점.
 *
 * 로그인이 메인 영역([MainKey])보다 항상 선행되도록, 로그인 여부에 따라
 * [LoginKey]와 [MainKey] 사이를 전환하는 별도의 root back stack을 관리한다.
 * 앱 시작 시 저장된 세션을 확인하는 동안에는 로딩 인디케이터를 보여준다.
 */
@Composable
fun GamssRootNavHost(
    rootViewModel: RootViewModel = hiltViewModel(),
) {
    val sessionState by rootViewModel.sessionState.collectAsState()

    when (sessionState) {
        SessionState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        SessionState.Authenticated -> RootBackStackNavDisplay(rootViewModel, startKey = MainKey)
        SessionState.Unauthenticated -> RootBackStackNavDisplay(rootViewModel, startKey = LoginKey)
    }
}

@Composable
private fun RootBackStackNavDisplay(
    rootViewModel: RootViewModel,
    startKey: NavKey,
) {
    val backStack = rememberNavBackStack(startKey)
    val shouldNavigateToLogin by rootViewModel.shouldNavigateToLogin.collectAsState()

    LaunchedEffect(shouldNavigateToLogin) {
        if (shouldNavigateToLogin) {
            backStack.clear()
            backStack.add(LoginKey)
            rootViewModel.onNavigatedToLogin()
        }
    }

    NavDisplay(
        entries = backStack.map(
            entryProvider {
                entry<LoginKey> {
                    LoginScreen(
                        googleWebClientId = stringResource(R.string.default_web_client_id),
                        onLoginSuccess = {
                            backStack.clear()
                            backStack.add(MainKey)
                        },
                    )
                }
                entry<MainKey> { GamssNavHost() }
            },
        ),
        onBack = {
            // 로그인 이전 화면으로는 되돌아가지 않는다.
            if (backStack.size > 1) {
                backStack.removeLastOrNull()
            }
        },
    )
}
