package com.gamss.android.feature.billing

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.gamss.android.domain.billing.PurchaseOutcome

/**
 * PurchasesUpdatedListener 콜백과 미처리 구매 복구 조회(queryPurchasesAsync)를 같은
 * [PurchaseOutcome] 타입으로 정규화한다. 두 경로 모두 [toOutcome]을 거치므로 호출 출처와
 * 무관하게 동일한 처리 결과를 낸다.
 */
object PurchaseResultMapper {

    /** PurchasesUpdatedListener 콜백 결과를 변환한다. */
    fun fromPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?): List<PurchaseOutcome> =
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> purchases.orEmpty().map { it.toOutcome() }
            BillingClient.BillingResponseCode.USER_CANCELED -> listOf(PurchaseOutcome.UserCanceled)
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> listOf(PurchaseOutcome.AlreadyOwned)
            else -> listOf(PurchaseOutcome.Failed(billingResult.debugMessage))
        }

    /**
     * ITEM_ALREADY_OWNED 복구 조회 결과를 변환한다. 미소비 구매가 여러 건일 수 있으므로 목록
     * 전체를 각각 변환한다 — 한 건만 원인이라고 단정하지 않는다.
     */
    fun fromRecoveredPurchases(purchases: List<Purchase>): List<PurchaseOutcome> = purchases.map { it.toOutcome() }

    private fun Purchase.toOutcome(): PurchaseOutcome {
        val productId = products.firstOrNull().orEmpty()
        return when (purchaseState) {
            Purchase.PurchaseState.PURCHASED -> PurchaseOutcome.Confirmed(
                productId = productId,
                purchaseToken = purchaseToken,
            )
            Purchase.PurchaseState.PENDING -> PurchaseOutcome.Pending
            else -> PurchaseOutcome.Failed("Unspecified purchase state for $productId")
        }
    }
}
