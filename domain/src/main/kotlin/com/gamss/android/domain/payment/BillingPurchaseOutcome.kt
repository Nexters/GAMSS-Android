package com.gamss.android.domain.payment

/**
 * Play 구매 콜백·미처리 구매 복구 조회 결과를 표현한다. [PendingPurchaseVerifier] 구현이 이 값을 보고
 * 검증·안내를 판단하므로 domain에 둔다 — 실제로 Play SDK 응답을 이 타입으로 바꾸는 작업은
 * core:billing(Play 어댑터)이 담당한다.
 */
sealed interface BillingPurchaseOutcome {
    data class Purchased(val purchaseToken: String, val productIds: List<String>) : BillingPurchaseOutcome
    data class Pending(val productIds: List<String>) : BillingPurchaseOutcome
    data object UserCanceled : BillingPurchaseOutcome
    data object AlreadyOwned : BillingPurchaseOutcome
    data class Error(
        val responseCode: Int,
        val message: String,
        val source: BillingErrorSource = BillingErrorSource.PURCHASE_UPDATE,
    ) : BillingPurchaseOutcome
}

enum class BillingErrorSource {
    PURCHASE_UPDATE,
    RESTORE_QUERY,
}
