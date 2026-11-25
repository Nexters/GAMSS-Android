package com.gamss.android.core.billing

import com.gamss.android.domain.payment.StoreBillingRepository
import com.gamss.android.domain.payment.StoreConnectionState
import com.gamss.android.domain.payment.StoreProductQueryResult
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class StoreBillingRepositoryImpl @Inject constructor(
    private val billing: BillingRepository,
) : StoreBillingRepository {
    override val connectionState: StateFlow<StoreConnectionState> = billing.connectionState

    override fun connect() = billing.connect()

    override suspend fun getProducts(productIds: List<String>): StoreProductQueryResult {
        val result = billing.queryProductDetails(productIds)
        return StoreProductQueryResult(result.callResult.isSuccess, result.products)
    }
}
