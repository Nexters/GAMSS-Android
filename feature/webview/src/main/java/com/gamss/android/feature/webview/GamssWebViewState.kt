package com.gamss.android.feature.webview

import android.os.Bundle
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * [GamssWebView]의 로딩 상태와 히스토리 제어를 화면에 노출하는 핸들.
 *
 * 화면이 WebView 인스턴스를 직접 다루지 않도록 필요한 동작만 열어둔다.
 */
@Stable
class GamssWebViewState internal constructor() {

    private var webView: WebView? = null

    /**
     * 로딩 여부.
     *
     * 초기값이 `false`인 이유는 WebView 콜백을 유일한 출처로 두기 위해서다. 첫 로드든 복원이든
     * `onPageStarted`가 곧바로 `true`로 올린다. 초기값을 `true`로 두면 콜백이 오지 않는 경로에서
     * 인디케이터가 화면에 영구히 남는다.
     */
    var isLoading: Boolean by mutableStateOf(false)
        internal set

    var hasError: Boolean by mutableStateOf(false)
        internal set

    fun canGoBack(): Boolean = webView?.canGoBack() == true

    fun goBack() {
        webView?.goBack()
    }

    /**
     * 현재 페이지를 다시 불러온다. 웹뷰가 없으면 에러 화면을 그대로 둔다.
     */
    fun reload() {
        val webView = webView ?: return
        hasError = false
        isLoading = true
        webView.reload()
    }

    internal fun attach(webView: WebView) {
        this.webView = webView
    }

    internal fun detach() {
        webView = null
    }

    internal fun saveInto(bundle: Bundle): Bundle {
        webView?.saveState(bundle)
        return bundle
    }
}

@Composable
fun rememberGamssWebViewState(): GamssWebViewState = remember { GamssWebViewState() }
