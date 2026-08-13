package com.gamss.android.feature.webview

import androidx.annotation.StringRes

/**
 * 앱이 웹뷰로 여는 페이지 목록.
 *
 * 새 페이지는 [GamssWebUrl]에 주소를 더하고 여기에 한 줄을 추가하면 된다.
 */
enum class GamssWebPage(val url: String, @param:StringRes val titleRes: Int) {
    ServiceTerms(GamssWebUrl.SERVICE_TERMS, R.string.webview_title_service_terms),
    PrivacyPolicy(GamssWebUrl.PRIVACY_POLICY, R.string.webview_title_privacy_policy),
    ;

    companion object {
        /**
         * 웹뷰 안에서 열도록 허용하는 호스트.
         *
         * 등록된 페이지에서 파생하므로 주소를 바꾸면 따라 바뀐다. 여기 없는 호스트는 외부 앱으로 넘긴다.
         */
        val ALLOWED_HOSTS: Set<String> = entries.mapNotNullTo(mutableSetOf()) { it.url.urlHostOrNull() }
    }
}
