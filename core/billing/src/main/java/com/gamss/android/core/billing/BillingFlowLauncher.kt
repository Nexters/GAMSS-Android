package com.gamss.android.core.billing

import com.gamss.android.domain.payment.StoreProduct

interface BillingFlowLauncher {
    fun launchBillingFlow(product: StoreProduct, accountToken: String): BillingOperationResult?
}
