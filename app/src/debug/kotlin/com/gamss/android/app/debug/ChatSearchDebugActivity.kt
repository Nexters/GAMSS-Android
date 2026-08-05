package com.gamss.android.app.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.feature.chatSearch.SearchChattingScreen
import dagger.hilt.android.AndroidEntryPoint

/** 채팅방 검색 디버그/검증 전용 화면. 프로덕션 아님. */
@AndroidEntryPoint
class ChatSearchDebugActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GamssTheme {
                SearchChattingScreen()
            }
        }
    }
}
