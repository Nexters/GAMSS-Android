package com.gamss.android.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * 토큰 사용량 새로고침 신호 버스 + 전역 사용량 캐시.
 *
 * [requestRefresh]로 재조회를 요청하면, 구현체가 내부에서 조회해 [usagePercent]/[isExhausted]를
 * 갱신하고 [alerts]로 흘려보낸다. LOW(10% 미만 남음)·EXHAUSTED(전부 소진) 모두 하루 1회만
 * 흘려보낸다 — 채팅방을 나갔다 들어오거나 다른 채팅방에 진입할 때마다 재조회가 일어나도 이미
 * 알린 알림은 중복으로 뜨지 않는다. [isExhausted]는 알림과 무관하게 조회될 때마다 항상
 * 최신값으로 갱신되므로, 화면은 이 값으로 입력창을 계속 잠글 수 있다. "이미 알렸는지"는
 * 화면(ViewModel) 로컬이 아니라 이 싱글턴 구현체가 기억한다.
 */
interface TokenUsageRefreshNotifier {
    val refreshEvents: Flow<Unit>

    /** 가장 최근에 조회된 사용률(0~100). 아직 한 번도 조회되지 않았으면 null. */
    val usagePercent: StateFlow<Int?>

    /**
     * 가장 최근 조회에서 서버가 확정한 소진 여부. 화면은 이 값으로 입력창·전송 버튼을 잠근다 —
     * 반올림 오차가 있는 [usagePercent] >= 100 보다 정확하다. 아직 한 번도 조회되지 않았으면
     * `false`(비소진으로 간주)다.
     */
    val isExhausted: StateFlow<Boolean>

    /** LOW·EXHAUSTED 모두 하루 1회만 흘려보내는 알림. */
    val alerts: Flow<TokenUsageAlert>

    fun requestRefresh()
}

enum class TokenUsageAlert {
    /** 오늘 사용 가능한 토큰이 10% 미만 남음. */
    LOW,

    /** 오늘 사용 가능한 토큰을 전부 사용함. */
    EXHAUSTED,
}
