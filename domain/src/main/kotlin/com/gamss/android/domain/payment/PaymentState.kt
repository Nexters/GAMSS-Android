package com.gamss.android.domain.payment

sealed interface PaymentEvent {
    data class Granted(val grantedTokens: Long) : PaymentEvent
    data class VerificationFailed(val reason: PaymentFailureReason) : PaymentEvent

    /** Play 결제 자체가 실패했다(구매 콜백의 오류 응답). 사용자 취소는 포함하지 않는다. */
    data object PurchaseFailed : PaymentEvent
}

/**
 * [notices]는 UI가 한 번씩 안내해야 할 결과 목록이다. 일회성 이벤트 대신 상태로 두어, 화면이 없을 때
 * 발생한 안내도 남아 있다가 표시되고, 세션이 바뀌면 상태와 함께 초기화되어 다른 계정에 노출되지 않는다.
 */
data class PaymentState(
    val isVerifying: Boolean = false,
    val paidBalance: Long? = null,
    val message: String? = null,
    val billingError: BillingPurchaseOutcome.Error? = null,
    val notices: List<PaymentNotice> = emptyList(),
)

data class PaymentNotice(val id: Long, val event: PaymentEvent)
