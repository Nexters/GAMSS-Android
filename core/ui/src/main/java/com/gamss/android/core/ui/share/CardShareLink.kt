package com.gamss.android.core.ui.share

/**
 * 카드 공유 링크. 주소는 이 파일에서만 바꾼다. 호출부에 URL 문자열을 직접 쓰지 않는다.
 */
object CardShareLink {

    /**
     * 공유 링크가 가리키는 웹 랜딩.
     */
    private const val LANDING_BASE_URL = "https://gamss.kr"

    /** [cardId] 카드를 여는 링크. */
    fun of(cardId: Long): String = "$LANDING_BASE_URL/cards/$cardId"
}
