package com.gamss.android.core.ui.share

/**
 * 카드 공유 링크. 주소는 이 파일에서만 바꾼다. 호출부에 URL 문자열을 직접 쓰지 않는다.
 */
object CardShareLink {

    /**
     * 공유 링크가 가리키는 웹 랜딩.
     *
     * TODO(도메인 확정): 실제 랜딩 주소로 바꿔야 한다. 지금 값은 절대 열리지 않는 예약 도메인
     *  (RFC 2606 의 `.invalid`)이라, 바꾸지 않은 채로 배포하면 카톡으로 보낸 링크가 열리지 않는다.
     */
    private const val LANDING_BASE_URL = "https://gamss.example.invalid"

    /** [cardId] 카드를 여는 링크. */
    fun of(cardId: Long): String = "$LANDING_BASE_URL/cards/$cardId"
}
