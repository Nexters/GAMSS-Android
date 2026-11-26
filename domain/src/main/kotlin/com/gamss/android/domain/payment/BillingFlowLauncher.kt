package com.gamss.android.domain.payment

interface BillingFlowLauncher {
    fun launchBillingFlow(product: StoreProduct, accountToken: String): BillingOperationResult?
}
