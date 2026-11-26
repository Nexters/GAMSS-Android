package com.gamss.android.core.billing

import com.android.billingclient.api.ProductDetails
import com.gamss.android.domain.payment.BillingOperationResult
import com.gamss.android.domain.payment.StoreConnectionState
import com.gamss.android.domain.payment.StoreProduct

typealias BillingConnectionState = StoreConnectionState

/**
 * [productDetails]는 방금 끝난 [BillingRepository.queryProductDetails] 호출이 돌려준 인스턴스 그대로다 —
 * 캐시된 오래된 참조가 아니라야 [BillingRepository.launchPurchaseFlow]가 stale ProductDetails로
 * 실패하지 않는다. billing SDK 타입이라 core:billing 모듈 밖에는 노출하지 않는다 — 호출부는
 * [productId]/[formattedPrice]만 보고, 이 값을 그대로 [BillingRepository.launchPurchaseFlow]에
 * 다시 넘기기만 하면 된다.
 */
data class BillingProduct(
    internal val productDetails: ProductDetails,
    override val productId: String,
    val offerToken: String?,
    override val formattedPrice: String,
) : StoreProduct

data class BillingProductQueryResult(
    val callResult: BillingOperationResult,
    val products: List<BillingProduct>,
    val unfetchedProductIds: List<String>,
)
