package com.gamss.android.feature.webview

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

private const val TAG = "GamssWebView"

internal class GamssWebViewClient(
    private val onLoadingChange: (Boolean) -> Unit,
    private val onError: () -> Unit,
) : WebViewClient() {

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        onLoadingChange(true)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        onLoadingChange(false)
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError,
    ) {
        // 이미지 같은 하위 리소스 실패로 화면 전체를 에러로 덮지 않는다.
        if (!request.isForMainFrame) return
        onLoadingChange(false)
        onError()
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val url = request.url.toString()
        // 호스트 허용 정책은 최상위 이동에만 적용한다. 하위 프레임까지 가로채면 임베드가 외부 앱으로 튄다.
        val handledByWebView = !request.isForMainFrame || WebViewUrlPolicy.isInAppUrl(url)

        if (!handledByWebView && WebViewUrlPolicy.isExternallyOpenable(url)) {
            view.context.openExternally(request.url)
        }
        // 넘길 곳이 없는 스킴은 웹뷰도 열지 않는다.
        return !handledByWebView
    }
}

// 웹뷰가 대신 열게 두면 허용 호스트 정책이 무의미해지므로, 받을 앱이 없으면 그냥 무시한다.
@Suppress("SwallowedException")
private fun Context.openExternally(uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        startActivity(intent)
    } catch (ignored: ActivityNotFoundException) {
        Log.w(TAG, "열 수 있는 앱이 없는 주소: $uri")
    }
}
