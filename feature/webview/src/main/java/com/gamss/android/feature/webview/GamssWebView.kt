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

/**
 * 앱 공통 웹뷰.
 *
 * 화면 전체를 차지하는 웹 페이지뿐 아니라 화면 일부에 끼워 넣는 용도로도 쓸 수 있다.
 *
 * 캐시는 세 겹으로 동작한다. 온라인에서는 서버 캐시 헤더를 따르고, 오프라인에서는 디스크 캐시를
 * 먼저 쓰며, 백스택에서 돌아왔을 때는 저장해 둔 히스토리와 스크롤 위치를 복원해 재로딩을 건너뛴다.
 */
@Composable
fun GamssWebView(
    url: String,
    state: GamssWebViewState,
    modifier: Modifier = Modifier,
) {
    val cacheMode = WebViewCachePolicy.cacheModeFor(rememberIsOnline()).toWebSettingsValue()

    // 주소가 바뀌면 히스토리와 저장 상태가 이어지면 안 되므로 웹뷰를 새로 만든다.
    key(url) {
        // 저장 시점에 살아 있는 WebView에서 상태를 뽑아야 구성 변경과 프로세스 종료를 모두 넘긴다.
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
                // destroy 는 뷰 트리에서 떼어낸 뒤 호출해야 한다.
                // AndroidView 는 이 블록을 먼저 부르고 나중에 떼므로 여기서 직접 뗀다.
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
        // 원격 문서에 로컬 파일과 content provider를 열어줄 이유가 없다.
        allowFileAccess = false
        allowContentAccess = false
        // https 문서가 http 리소스를 섞어 받지 않게 막는다.
        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        // 새 창은 허용 호스트 검사를 우회하는 경로가 되므로 열지 않는다.
        setSupportMultipleWindows(false)
        javaScriptCanOpenWindowsAutomatically = false
    }
    overScrollMode = WebView.OVER_SCROLL_NEVER
}

private fun WebViewCacheMode.toWebSettingsValue(): Int = when (this) {
    WebViewCacheMode.Default -> WebSettings.LOAD_DEFAULT
    WebViewCacheMode.CacheElseNetwork -> WebSettings.LOAD_CACHE_ELSE_NETWORK
}
