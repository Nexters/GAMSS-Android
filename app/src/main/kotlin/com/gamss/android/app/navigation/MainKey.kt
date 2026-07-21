package com.gamss.android.app.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * 로그인 이후 진입하는 BottomBar 기반 메인 영역을 가리키는 key.
 */
@Serializable
data object MainKey : NavKey
