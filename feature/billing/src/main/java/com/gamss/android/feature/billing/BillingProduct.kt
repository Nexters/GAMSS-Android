package com.gamss.android.feature.billing

import com.android.billingclient.api.ProductDetails
import com.gamss.android.domain.billing.BillingProductInfo

/** Play 의 구매 옵션 하나. [formattedPrice]는 Play 원문 그대로 표시한다 — 하드코딩하지 않는다. */
data class BillingPurchaseOption(
    val offerToken: String,
    val formattedPrice: String,
)

/**
 * 서버 상품 메타와 Play [ProductDetails] 조회 결과를 합친 화면 표시용 모델.
 * [productDetails]가 null 이면 Play 조회에 실패(또는 누락)했다는 뜻이고, 이때 [isPurchasable]은
 * 항상 false 다 — 구매를 막는다.
 */
data class BillingProduct(
    val productId: String,
    val benefitDescription: String,
    val productDetails: ProductDetails?,
    val options: List<BillingPurchaseOption>,
) {
    val isPurchasable: Boolean get() = productDetails != null && options.isNotEmpty()
}

/**
 * 서버 목록을 기준으로 Play 조회 결과를 맞춰 붙인다. 서버에만 있고 Play 조회에 실패/누락된
 * 상품도 목록에는 남기되 [BillingProduct.isPurchasable]을 false 로 둬 화면에서 구매를 막는다.
 */
fun mergeBillingProducts(
    serverProducts: List<BillingProductInfo>,
    playProductDetails: List<ProductDetails>,
): List<BillingProduct> {
    val detailsByProductId = playProductDetails.associateBy { it.productId }
    return serverProducts.map { info ->
        val details = detailsByProductId[info.productId]
        BillingProduct(
            productId = info.productId,
            benefitDescription = info.benefitDescription,
            productDetails = details,
            options = details?.toPurchaseOptions().orEmpty(),
        )
    }
}

private fun ProductDetails.toPurchaseOptions(): List<BillingPurchaseOption> =
    oneTimePurchaseOfferDetailsList.orEmpty().map { offer ->
        BillingPurchaseOption(
            offerToken = offer.offerToken.orEmpty(),
            formattedPrice = offer.formattedPrice.orEmpty(),
        )
    }
