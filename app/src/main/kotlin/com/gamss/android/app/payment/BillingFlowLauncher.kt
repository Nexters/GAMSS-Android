package com.gamss.android.app.payment

import com.gamss.android.core.billing.BillingOperationResult
import com.gamss.android.domain.payment.StoreProduct

interface BillingFlowLauncher {
    fun launchBillingFlow(product: StoreProduct, accountToken: String): BillingOperationResult?
}
