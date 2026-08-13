package com.gamss.android.feature.webview

import java.net.URI

/**
 * 허용 목록에 없는 호스트를 앱 안에서 열면 피싱과 탈취 경로가 된다.
 * 안드로이드 API에 의존하지 않게 두어 단위 테스트로 검증한다.
 */
object WebViewUrlPolicy {

    private val IN_APP_SCHEMES = setOf("http", "https")

    // 원격 문서가 임의의 앱 딥링크(market:, sms:, 사설 스킴)를 띄우지 못하게 좁혀 둔다.
    private val EXTERNAL_SCHEMES = setOf("http", "https", "mailto", "tel")

    fun isInAppUrl(url: String, allowedHosts: Set<String> = GamssWebPage.ALLOWED_HOSTS): Boolean {
        val uri = url.asUriOrNull() ?: return false
        val scheme = uri.scheme?.lowercase().orEmpty()
        val host = uri.host?.lowercase().orEmpty()
        return scheme in IN_APP_SCHEMES && host in allowedHosts
    }

    fun isExternallyOpenable(url: String): Boolean =
        url.asUriOrNull()?.scheme?.lowercase().orEmpty() in EXTERNAL_SCHEMES
}

internal fun String.urlHostOrNull(): String? = asUriOrNull()?.host?.lowercase()

private fun String.asUriOrNull(): URI? = runCatching { URI(this) }.getOrNull()
