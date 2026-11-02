package com.gamss.android.data.repository

import com.gamss.android.core.common.AppResult
import com.gamss.android.data.remote.billing.BillingService
import com.gamss.android.data.remote.billing.model.request.VerifyPurchaseRequest
import com.gamss.android.data.remote.billing.model.response.toDomain
import com.gamss.android.data.remote.runCatchingApiCall
import com.gamss.android.data.remote.throwIfFailed
import com.gamss.android.domain.billing.BillingProductInfo
import com.gamss.android.domain.billing.BillingRepository
import com.gamss.android.domain.billing.GrantResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class BillingRepositoryImpl @Inject constructor(
    private val billingService: BillingService,
) : BillingRepository {

    override suspend fun getServerProducts(): AppResult<List<BillingProductInfo>> = runCatchingApiCall {
        val response = billingService.getProducts()
        response.throwIfFailed()
        checkNotNull(response.data) { "No available billing product data" }.map { it.toDomain() }
    }

    override suspend fun verifyAndGrantPurchase(
        productId: String,
        purchaseToken: String,
    ): AppResult<GrantResult> = runCatchingApiCall {
        val response = billingService.verifyPurchase(
            VerifyPurchaseRequest(productId = productId, purchaseToken = purchaseToken),
        )
        response.throwIfFailed()
        checkNotNull(response.data) { "No available purchase verification data" }.toDomain()
    }
}
