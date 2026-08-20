package com.gamss.android.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * 토큰 사용량 새로고침 신호 버스 + 전역 사용량 캐시.
 *
 * [requestRefresh]로 재조회를 요청하면, 구현체가 내부에서 조회해 [usagePercent]를 갱신하고
 * [alerts]로 흘려보낸다. LOW(10% 미만 남음)는 하루 1회만, EXHAUSTED(전부 소진)는 소진 상태로
 * 재조회될 때마다 매번 흘려보낸다 — 두 알림의 발행 정책이 다르다. 여러 채팅방을 오가도(대화방을
 * 나갔다 들어와도) LOW 알림이 중복으로 뜨지 않도록, "이미 알렸는지"는 화면(ViewModel) 로컬이
 * 아니라 이 싱글턴 구현체가 기억한다.
 */
interface TokenUsageRefreshNotifier {
    val refreshEvents: Flow<Unit>

    /** 가장 최근에 조회된 사용률(0~100). 아직 한 번도 조회되지 않았으면 null. */
    val usagePercent: StateFlow<Int?>

    /** LOW는 하루 1회만, EXHAUSTED는 소진 상태가 감지될 때마다 매번 흘려보내는 알림. */
    val alerts: Flow<TokenUsageAlert>

    fun requestRefresh()
}

enum class TokenUsageAlert {
    /** 오늘 사용 가능한 토큰이 10% 미만 남음. */
    LOW,

    /** 오늘 사용 가능한 토큰을 전부 사용함. */
    EXHAUSTED,
}
