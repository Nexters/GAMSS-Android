package com.gamss.android.core.billing

import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase

/**
 * 구매 콜백(`PurchasesUpdatedListener.onPurchasesUpdated`)과 미처리 구매 복구 조회
 * (`queryPurchasesAsync`) 결과를 동일한 [BillingPurchaseOutcome] 집합으로 매핑한다. 두 경로가
 * 이 함수 하나를 공유해야 상태(OK+PURCHASED/PENDING, USER_CANCELED, ITEM_ALREADY_OWNED)가
 * 어디서 와도 같은 enum 조합으로 처리된다. 미소비 구매가 여러 개 섞여 있을 수 있어 구매 단위로
 * 하나씩 매핑한다.
 */
internal fun mapToPurchaseOutcomes(
    billingResult: BillingResult,
    purchases: List<Purchase>?,
): List<BillingPurchaseOutcome> = when (billingResult.responseCode) {
    BillingResponseCode.OK -> purchases.orEmpty().map { it.toOutcome() }
    BillingResponseCode.USER_CANCELED -> listOf(BillingPurchaseOutcome.UserCanceled)
    // ITEM_ALREADY_OWNED는 구매 자체가 아니라 launchBillingFlow 실패 코드라 purchases가 비어
    // 온다 — 미처리 구매가 무엇인지는 호출부가 queryUnfinishedPurchases()로 다시 조회해야 한다.
    BillingResponseCode.ITEM_ALREADY_OWNED -> listOf(BillingPurchaseOutcome.AlreadyOwned)
    else -> listOf(
        BillingPurchaseOutcome.Error(
            responseCode = billingResult.responseCode,
            message = billingResult.debugMessage,
        ),
    )
}

private fun Purchase.toOutcome(): BillingPurchaseOutcome = when (purchaseState) {
    Purchase.PurchaseState.PURCHASED -> BillingPurchaseOutcome.Purchased(
        purchaseToken = purchaseToken,
        productIds = products,
    )
    Purchase.PurchaseState.PENDING -> BillingPurchaseOutcome.Pending(productIds = products)
    else -> BillingPurchaseOutcome.Error(
        responseCode = BillingResponseCode.ERROR,
        message = "Unknown purchase state($purchaseState) for order $orderId",
    )
}
