package com.gamss.android.app

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.gamss.android.app.navigation.GamssRootNavHost
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.core.ui.share.clearStaleInstagramStoryShareCache
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        applyOverrideConfiguration(
            Configuration().apply {
                fontScale = 0f
                uiMode = Configuration.UI_MODE_NIGHT_NO
            },
        )
        super.attachBaseContext(newBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // keepOnScreenCondition 으로 세션을 기다리지 않는다. 첫 프레임 draw 를 막아 TTID 에
        // 복원 대기가 그대로 들어가고, 토큰 재발급이 네트워크를 타면 정지한 화면이 된다.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // light 는 다크 고정이 아니라 대비 스크림을 끄는 선택이다. auto 로 두면 시스템이
        // 내비게이션 바에 흰 스크림을 깔아 종이 배경이 끊긴다.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.BLACK),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.BLACK),
        )
        setContent {
            GamssTheme {
                GamssRootNavHost()
            }
        }
    }

    // 인스타그램에서 돌아와 화면이 다시 보일 때마다 지운다. 최초 실행 때도 onCreate 직후
    // onResume 이 불려서 앱 시작 시점 정리도 그대로 진행됨.
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch(Dispatchers.IO) { clearStaleInstagramStoryShareCache() }
    }
}
