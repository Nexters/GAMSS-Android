package com.gamss.android.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
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
 */
@Composable
fun GamssRootNavHost() {
    val backStack = rememberNavBackStack(LoginKey)

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
