package com.gamss.android.feature.webview

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun GamssWebView(
    url: String,
    state: GamssWebViewState,
    modifier: Modifier = Modifier,
) {
    val cacheMode = WebViewCachePolicy.cacheModeFor(rememberIsOnline()).toWebSettingsValue()

    // 주소가 바뀌면 히스토리와 저장 상태가 이어지면 안 된다.
    key(url) {
        // 저장 시점에 살아 있는 WebView에서 뽑아야 구성 변경과 프로세스 종료를 모두 넘긴다.
        val savedState = rememberSaveable(
            saver = Saver<Bundle, Bundle>(
                save = { state.saveInto(it) },
                restore = { it },
            ),
        ) { Bundle() }

        AndroidView(
            modifier = modifier,
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    // 캐시 모드는 첫 로드 전에 정해져야 오프라인 폴백이 첫 진입부터 걸린다.
                    configureForGamss(cacheMode)
                    webViewClient = GamssWebViewClient(
                        onLoadingChange = { loading ->
                            state.isLoading = loading
                            if (loading) state.hasError = false
                        },
                        onError = { state.hasError = true },
                    )
                    state.attach(this)

                    if (savedState.isEmpty || restoreState(savedState) == null) {
                        loadUrl(url)
                    }
                }
            },
            update = { webView -> webView.settings.cacheMode = cacheMode },
            onRelease = { webView ->
                state.saveInto(savedState)
                state.detach()
                webView.stopLoading()
                // destroy 는 뷰 트리에서 뗀 뒤 호출해야 한다. AndroidView 는 이 블록을 먼저 부른다.
                (webView.parent as? ViewGroup)?.removeView(webView)
                webView.destroy()
            },
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun WebView.configureForGamss(cacheMode: Int) {
    with(settings) {
        javaScriptEnabled = true
        domStorageEnabled = true
        this.cacheMode = cacheMode
        // 원격 문서에 로컬 리소스를 열어주지 않고, 새 창으로 허용 호스트 검사를 우회하지 못하게 막는다.
        allowFileAccess = false
        allowContentAccess = false
        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        setSupportMultipleWindows(false)
        javaScriptCanOpenWindowsAutomatically = false
    }
    overScrollMode = WebView.OVER_SCROLL_NEVER
}

private fun WebViewCacheMode.toWebSettingsValue(): Int = when (this) {
    WebViewCacheMode.Default -> WebSettings.LOAD_DEFAULT
    WebViewCacheMode.CacheElseNetwork -> WebSettings.LOAD_CACHE_ELSE_NETWORK
}
