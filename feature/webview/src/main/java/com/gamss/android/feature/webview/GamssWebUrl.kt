package com.gamss.android.feature.webview

/**
 * 주소는 이 파일에서만 바꾼다. 호출부에 URL 문자열을 직접 쓰지 않는다.
 */
object GamssWebUrl {

    // 약관과 처리방침이 한 문서에 함께 실려 있다. 분리되면 아래 두 상수를 각각 다른 주소로 바꾼다.
    private const val TERMS_AND_PRIVACY =
        "https://shadow-bridge-c40.notion.site/GAMSS-3bb88bba374f80549b3ef9fdfacb7584"

    const val SERVICE_TERMS = TERMS_AND_PRIVACY

    const val PRIVACY_POLICY = TERMS_AND_PRIVACY
}
