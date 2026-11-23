package com.gamss.android.core.billing

import com.android.billingclient.api.ProductDetails

enum class BillingConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
}

/**
 * [productDetails]는 방금 끝난 [BillingRepository.queryProductDetails] 호출이 돌려준 인스턴스 그대로다 —
 * 캐시된 오래된 참조가 아니라야 [BillingRepository.launchPurchaseFlow]가 stale ProductDetails로
 * 실패하지 않는다. billing SDK 타입이라 core:billing 모듈 밖에는 노출하지 않는다 — 호출부는
 * [productId]/[formattedPrice]만 보고, 이 값을 그대로 [BillingRepository.launchPurchaseFlow]에
 * 다시 넘기기만 하면 된다.
 */
data class BillingProduct(
    internal val productDetails: ProductDetails,
    val productId: String,
    val offerToken: String?,
    val formattedPrice: String,
)

/**
 * 성공 여부는 [core:billing] 안에서 이미 판정해서 담아준다 — 호출부는 [isSuccess]만 보고
 * 분기하면 되고, SDK 응답 코드 상수를 몰라도 된다. [responseCode]는 해석하지 않고 로그 등
 * 참고용으로만 쓴다.
 */
data class BillingOperationResult(
    val isSuccess: Boolean,
    val responseCode: Int,
)

data class BillingProductQueryResult(
    val callResult: BillingOperationResult,
    val products: List<BillingProduct>,
    val unfetchedProductIds: List<String>,
)

sealed interface BillingPurchaseOutcome {
    data class Purchased(val purchaseToken: String, val productIds: List<String>) : BillingPurchaseOutcome
    data class Pending(val productIds: List<String>) : BillingPurchaseOutcome
    data object UserCanceled : BillingPurchaseOutcome
    data object AlreadyOwned : BillingPurchaseOutcome
    data class Error(val responseCode: Int, val message: String) : BillingPurchaseOutcome
}
