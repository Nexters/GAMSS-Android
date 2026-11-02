package com.gamss.android.feature.billing

import com.android.billingclient.api.ProductDetails

sealed interface BillingSideEffect {

    /**
     * Screen이 받아 메인 스레드에서 launchBillingFlow를 호출해야 한다. 즉시 반환되는
     * BillingResult는 [BillingViewModel.onBillingFlowLaunched]로 다시 넘겨야 한다.
     */
    data class LaunchPurchase(
        val productDetails: ProductDetails,
        val offerToken: String,
        val obfuscatedAccountId: String,
    ) : BillingSideEffect

    data class ShowError(val message: String?) : BillingSideEffect
}
