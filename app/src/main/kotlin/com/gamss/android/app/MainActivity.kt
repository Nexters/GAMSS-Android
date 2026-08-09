package com.gamss.android.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gamss.android.app.navigation.GamssRootNavHost
import com.gamss.android.core.common.BuildInfo
import com.gamss.android.core.designsystem.theme.GamssTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var buildInfo: BuildInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GamssTheme {
                GamssRootNavHost(isDebug = buildInfo.isDebug)
            }
        }
    }
}
