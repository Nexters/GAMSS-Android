package com.gamss.android.app

import android.content.Context
import android.content.res.Configuration
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
}
