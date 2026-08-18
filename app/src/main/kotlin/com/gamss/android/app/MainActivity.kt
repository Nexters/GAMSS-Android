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
        // 인자 없는 기본값은 시스템 바 아이콘 색을 기기 다크 모드에 맞춘다. 하지만 셸이 라이트로
        // 고정돼 있어(MainScreen 참고) 기기가 다크면 흰 배경에 흰 아이콘이 겹친다. 다크 시안이
        // 나와 셸 고정을 풀 때 이 스타일도 함께 되돌린다.
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
}
