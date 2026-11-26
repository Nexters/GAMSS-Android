package com.gamss.android.domain.payment

import kotlinx.coroutines.flow.StateFlow

/**
 * 앱 프로세스에서 구매 결과를 수신하고 서버 검증과 복구를 담당한다. 모든 메서드는 어느 스레드에서 호출해도
 * 된다 — 구현체는 자체 스코프 안에서만 내부 상태를 바꾼다.
 */
interface PendingPurchaseVerifier {
    val state: StateFlow<PaymentState>

    /** 앱이 포그라운드로 돌아올 때(ProcessLifecycleOwner onStart 등) 호출한다. */
    fun requestRecovery()

    /** UI가 [PaymentState.notices]의 안내를 표시한 뒤 호출한다. 호출 전까지 안내는 상태에 남는다. */
    fun onNoticeShown(noticeId: Long)
}
