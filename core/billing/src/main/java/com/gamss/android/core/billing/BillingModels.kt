package com.gamss.android.core.billing

import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails

enum class BillingConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    CLOSED,
}

/**
 * [productDetails]는 방금 끝난 [BillingRepository.queryProductDetails] 호출이 돌려준 인스턴스 그대로다 —
 * 캐시된 오래된 참조가 아니라야 [BillingRepository.launchPurchaseFlow]가 stale ProductDetails로
 * 실패하지 않는다.
 */
data class BillingProduct(
    val productDetails: ProductDetails,
    val offerToken: String?,
    val formattedPrice: String,
)

data class BillingProductQueryResult(
    val billingResult: BillingResult,
    val products: List<BillingProduct>,
    val unfetchedProductIds: List<String>,
)

sealed interface BillingPurchaseOutcome {
    data class Purchased(val purchaseToken: String, val productIds: List<String>) : BillingPurchaseOutcome
    data class Pending(val productIds: List<String>) : BillingPurchaseOutcome
    data object UserCanceled : BillingPurchaseOutcome
    data class AlreadyOwned(val productIds: List<String>) : BillingPurchaseOutcome
    data class Error(val responseCode: Int, val message: String) : BillingPurchaseOutcome
}
