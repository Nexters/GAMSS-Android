package com.gamss.android.domain.payment

import kotlinx.coroutines.flow.StateFlow

/**
 * 앱 프로세스에서 구매 결과를 수신하고 서버 검증과 복구를 담당한다. 모든 메서드는 어느 스레드에서 호출해도
 * 된다 — 구현체는 자체 스코프 안에서만 내부 상태를 바꾼다.
 */
interface PendingPurchaseVerifier {
    val state: StateFlow<PaymentState>

    /**
     * 미처리 구매(INAPP)를 다시 조회해 남은 구매를 서버 검증으로 보낸다. 앱 시작과 포그라운드 복귀(app의
     * ProcessLifecycleOwner), 상점 진입, 결제창의 ITEM_ALREADY_OWNED 응답 때 호출한다. Billing이 연결되지 않았으면
     * 연결부터 시작하고, 연결에 성공하면 조회한다.
     */
    fun requestRecovery()

    /** UI가 [PaymentState.notices]의 안내를 표시한 뒤 호출한다. 호출 전까지 안내는 상태에 남는다. */
    fun onNoticeShown(noticeId: Long)
}
