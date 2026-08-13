package com.gamss.android.feature.webview

/**
 * 웹뷰가 쓸 캐시 모드.
 *
 * 안드로이드 상수로 바로 두지 않는 이유는 정책을 순수 코틀린으로 두어 단위 테스트하기 위해서다.
 * 실제 [android.webkit.WebSettings] 값으로의 변환은 웹뷰를 만드는 쪽에서 한다.
 */
enum class WebViewCacheMode {
    /** 서버가 준 캐시 헤더를 따른다. */
    Default,

    /** 캐시가 있으면 캐시를, 없으면 네트워크를 쓴다. */
    CacheElseNetwork,
}

/**
 * 온라인에서는 캐시 헤더를 따르고, 오프라인에서는 마지막으로 받은 문서라도 보여준다.
 */
object WebViewCachePolicy {

    fun cacheModeFor(isOnline: Boolean): WebViewCacheMode =
        if (isOnline) WebViewCacheMode.Default else WebViewCacheMode.CacheElseNetwork
}
