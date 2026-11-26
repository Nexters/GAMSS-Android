package com.gamss.android.data.repository

import android.util.Log
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

private const val TAG = "PaymentRepositoryImpl"

/** 앱이 모르는 상태값은 예외로 실패시키지 않고 [PaymentStatus.UNKNOWN]으로 받아 로그를 남긴다. */
private fun String.toPaymentStatus(): PaymentStatus =
    PaymentStatus.entries.firstOrNull { it != PaymentStatus.UNKNOWN && it.name == this } ?: run {
        Log.w(TAG, "알 수 없는 구매 검증 상태입니다: status=$this")
        PaymentStatus.UNKNOWN
    }

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
        PaymentGrant(data.status.toPaymentStatus(), data.productId, data.grantedTokens, data.paidBalance)
    }
}
