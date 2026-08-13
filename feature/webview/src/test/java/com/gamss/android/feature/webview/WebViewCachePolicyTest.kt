package com.gamss.android.feature.webview

import org.junit.Assert.assertEquals
import org.junit.Test

class WebViewCachePolicyTest {

    @Test
    fun `온라인이면 서버 캐시 헤더를 따른다`() {
        assertEquals(WebViewCacheMode.Default, WebViewCachePolicy.cacheModeFor(isOnline = true))
    }

    @Test
    fun `오프라인이면 캐시를 먼저 쓴다`() {
        assertEquals(
            WebViewCacheMode.CacheElseNetwork,
            WebViewCachePolicy.cacheModeFor(isOnline = false),
        )
    }
}
