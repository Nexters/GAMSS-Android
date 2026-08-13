package com.gamss.android.feature.webview

import androidx.annotation.StringRes

enum class GamssWebPage(val url: String, @param:StringRes val titleRes: Int) {
    ServiceTerms(GamssWebUrl.SERVICE_TERMS, R.string.webview_title_service_terms),
    PrivacyPolicy(GamssWebUrl.PRIVACY_POLICY, R.string.webview_title_privacy_policy),
    ;

    companion object {
        // 여기 없는 호스트는 웹뷰가 열지 않고 외부 앱으로 넘긴다.
        val ALLOWED_HOSTS: Set<String> = entries.mapNotNullTo(mutableSetOf()) { it.url.urlHostOrNull() }
    }
}
