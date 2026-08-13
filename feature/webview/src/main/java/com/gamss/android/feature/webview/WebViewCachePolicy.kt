package com.gamss.android.feature.webview

/**
 * [android.webkit.WebSettings] 상수를 바로 쓰지 않는 이유는 정책을 순수 코틀린으로 두어
 * 단위 테스트하기 위해서다. 실제 값 변환은 웹뷰를 만드는 쪽에서 한다.
 */
enum class WebViewCacheMode {
    Default,
    CacheElseNetwork,
}

object WebViewCachePolicy {

    fun cacheModeFor(isOnline: Boolean): WebViewCacheMode =
        if (isOnline) WebViewCacheMode.Default else WebViewCacheMode.CacheElseNetwork
}
