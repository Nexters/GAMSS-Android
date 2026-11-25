package com.gamss.android.core.billing

import android.app.Activity
import android.content.Context
import android.os.Looper
import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryProductDetailsResult
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryPurchasesAsync
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/** [BillingClient]를 아는 유일한 클래스. 생성과 동시에 자신을 리스너로 등록해, 생성 순서 문제 없이
 * 콜백을 바로 [connectionEvents]/[purchaseUpdates] Flow로 흘려보낸다. */
@Singleton
internal class PlayBillingDataSourceImpl @Inject constructor(
    @ApplicationContext context: Context,
) : PlayBillingDataSource, PurchasesUpdatedListener {

    private val connectionEventsFlow = MutableSharedFlow<BillingConnectionEvent>(extraBufferCapacity = 1)
    private val purchaseUpdatesFlow =
        MutableSharedFlow<PurchasesUpdatedEvent>(extraBufferCapacity = PURCHASE_UPDATES_BUFFER)

    override val connectionEvents: Flow<BillingConnectionEvent> = connectionEventsFlow.asSharedFlow()
    override val purchaseUpdates: Flow<PurchasesUpdatedEvent> = purchaseUpdatesFlow.asSharedFlow()

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    override fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                connectionEventsFlow.tryEmit(BillingConnectionEvent.SetupFinished(billingResult))
            }

            override fun onBillingServiceDisconnected() {
                connectionEventsFlow.tryEmit(BillingConnectionEvent.ServiceDisconnected)
            }
        })
    }

    override suspend fun queryProductDetails(productIds: List<String>): Pair<BillingResult, QueryProductDetailsResult> {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                productIds.map { productId ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                },
            )
            .build()

        return suspendCancellableCoroutine { continuation ->
            billingClient.queryProductDetailsAsync(params) { resultCode, queryResult ->
                continuation.resume(resultCode to queryResult)
            }
        }
    }

    override fun launchBillingFlow(
        activity: Activity,
        product: BillingProduct,
        obfuscatedAccountId: String,
    ): BillingResult {
        logPurchaseParameters(product, obfuscatedAccountId)
        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(product.productDetails)
            .apply { product.offerToken?.let(::setOfferToken) }
            .build()

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .setObfuscatedAccountId(obfuscatedAccountId)
            .build()

        return billingClient.launchBillingFlow(activity, billingFlowParams).also {
            logResult("launchBillingFlow", it)
        }
    }

    override suspend fun queryPurchases(): PurchasesResult {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        return billingClient.queryPurchasesAsync(params).also {
            logResult("queryPurchasesAsync", it.billingResult)
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        logResult("onPurchasesUpdated", billingResult)
        purchaseUpdatesFlow.tryEmit(PurchasesUpdatedEvent(billingResult, purchases))
    }

    // adb의 log.tag.GamssBilling을 DEBUG로 설정했을 때만 활성화한다. 토큰 원문은 기록하지 않는다.
    private fun logPurchaseParameters(product: BillingProduct, accountId: String) {
        if (!Log.isLoggable(DIAGNOSTIC_TAG, Log.DEBUG)) return
        val details = product.productDetails
        val offerMatches = details.oneTimePurchaseOfferDetailsList.orEmpty().any {
            it.offerToken == product.offerToken
        }
        Log.d(
            DIAGNOSTIC_TAG,
            "purchase parameters: productId=${details.productId}, type=${details.productType}, " +
                "productIdMatches=${details.productId == product.productId}, productCount=1, " +
                "offerMatches=$offerMatches, accountIdLength=${accountId.length}, " +
                "accountIdIsUuid=${UUID_PATTERN.matches(accountId)}, " +
                "isReady=${billingClient.isReady}, mainThread=${Looper.myLooper() == Looper.getMainLooper()}",
        )
    }

    private fun logResult(operation: String, result: BillingResult) {
        if (Log.isLoggable(DIAGNOSTIC_TAG, Log.DEBUG)) {
            Log.d(DIAGNOSTIC_TAG, "$operation: code=${result.responseCode}, detail=${result.debugMessage}")
        }
    }

    private companion object {
        const val DIAGNOSTIC_TAG = "GamssBilling"
        val UUID_PATTERN = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
        const val PURCHASE_UPDATES_BUFFER = 8
    }
}
