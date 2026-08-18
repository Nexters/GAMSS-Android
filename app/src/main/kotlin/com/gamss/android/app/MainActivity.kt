package com.gamss.android.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gamss.android.app.navigation.GamssRootNavHost
import com.gamss.android.core.designsystem.theme.GamssTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 시스템 바 아이콘과 테마를 모두 라이트로 고정한다. 둘 중 하나만 고정하면 다크 모드 기기에서
        // 검은 배경에 검은 아이콘이 겹친다. 다크 시안이 나오면 이 고정과 MainScreen 의 고정을 함께 푼다.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.BLACK),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.BLACK),
        )
        setContent {
            GamssTheme(darkTheme = false) {
                GamssRootNavHost()
            }
        }
    }
}
