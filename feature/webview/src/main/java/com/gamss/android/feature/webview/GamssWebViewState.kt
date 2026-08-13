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
 * 화면이 WebView 인스턴스를 직접 다루지 않도록 필요한 동작만 열어둔다.
 */
@Stable
class GamssWebViewState internal constructor() {

    private var webView: WebView? = null

    // 초기값을 true 로 두면 콜백이 오지 않는 경로에서 인디케이터가 영구히 남는다.
    var isLoading: Boolean by mutableStateOf(false)
        internal set

    var hasError: Boolean by mutableStateOf(false)
        internal set

    fun canGoBack(): Boolean = webView?.canGoBack() == true

    fun goBack() {
        webView?.goBack()
    }

    fun reload() {
        // 웹뷰가 없으면 에러 화면을 지우지 않는다. 지우면 빈 화면에 재시도 수단만 사라진다.
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
