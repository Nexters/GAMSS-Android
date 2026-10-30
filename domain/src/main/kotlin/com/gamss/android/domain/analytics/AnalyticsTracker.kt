package com.gamss.android.domain.analytics

/** 속성에는 대화 본문, 카드 내용, 닉네임처럼 사용자가 쓴 내용을 넣지 않는다. */
interface AnalyticsTracker {
    fun track(event: String, properties: Map<String, Any> = emptyMap())
}
