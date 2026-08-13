package com.gamss.android.feature.webview

/**
 * 앱이 웹뷰로 여는 주소의 단일 출처.
 *
 * 주소는 이 파일에서만 바꾼다. 호출부에 URL 문자열을 직접 쓰지 않는다.
 */
object GamssWebUrl {

    const val SERVICE_TERMS =
        "https://shadow-bridge-c40.notion.site/GAMSS-3bb88bba374f80549b3ef9fdfacb7584"

    // 전용 페이지가 아직 없어 워크스페이스 루트를 가리킨다. 페이지가 나오면 이 주소만 바꾼다.
    const val PRIVACY_POLICY = "https://shadow-bridge-c40.notion.site/"
}
