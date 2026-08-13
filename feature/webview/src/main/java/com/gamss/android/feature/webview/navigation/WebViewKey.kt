package com.gamss.android.feature.webview.navigation

import androidx.navigation3.runtime.NavKey
import com.gamss.android.feature.webview.GamssWebPage
import kotlinx.serialization.Serializable

/**
 * 웹뷰 화면 진입 키.
 *
 * 주소가 아니라 페이지 식별자만 담는다. 호출부가 URL을 알 필요가 없고,
 * 주소가 바뀌어도 저장된 백스택이 깨지지 않는다.
 */
@Serializable
data class WebViewKey(val page: GamssWebPage) : NavKey
