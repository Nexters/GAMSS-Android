package com.gamss.android.data.repository

import com.gamss.android.core.common.AppResult
import com.gamss.android.data.remote.payment.PaymentService
import com.gamss.android.data.remote.payment.model.request.GooglePurchaseRequest
import com.gamss.android.data.remote.runCatchingApiCall
import com.gamss.android.data.remote.throwIfFailed
import com.gamss.android.domain.payment.PaymentGrant
import com.gamss.android.domain.payment.PaymentRepository
import com.gamss.android.domain.payment.PaymentStatus
import com.gamss.android.domain.payment.TokenProduct
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class PaymentRepositoryImpl @Inject constructor(
    private val service: PaymentService,
) : PaymentRepository {
    override suspend fun getProducts(): AppResult<List<TokenProduct>> = runCatchingApiCall {
        val response = service.getProducts()
        response.throwIfFailed()
        checkNotNull(response.data).map { TokenProduct(it.productId, it.tokenAmount) }
    }

    override suspend fun getAccountToken(): AppResult<String> = runCatchingApiCall {
        val response = service.getAccountToken()
        response.throwIfFailed()
        checkNotNull(response.data).accountToken.also { check(it.isNotBlank()) }
    }

    override suspend fun verifyGooglePurchase(
        productId: String,
        purchaseToken: String,
    ): AppResult<PaymentGrant> = runCatchingApiCall {
        val response = service.verifyGooglePurchase(GooglePurchaseRequest(productId, purchaseToken))
        response.throwIfFailed()
        val data = checkNotNull(response.data)
        check(data.productId == productId) { "Unexpected payment product" }
        PaymentGrant(PaymentStatus.valueOf(data.status), data.productId, data.grantedTokens, data.paidBalance)
    }
}
