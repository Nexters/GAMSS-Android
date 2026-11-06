package com.gamss.android.core.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryProductDetailsResult
import com.android.billingclient.api.QueryPurchasesParams
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

    override fun endConnection() = billingClient.endConnection()

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
        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(product.productDetails)
            .apply { product.offerToken?.let(::setOfferToken) }
            .build()

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .setObfuscatedAccountId(obfuscatedAccountId)
            .build()

        return billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    override suspend fun queryPurchases(): Pair<BillingResult, List<Purchase>> {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        return suspendCancellableCoroutine { continuation ->
            billingClient.queryPurchasesAsync(params) { result, purchaseList ->
                continuation.resume(result to purchaseList)
            }
        }
    }

    override suspend fun consumePurchase(purchaseToken: String): BillingResult {
        val params = ConsumeParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()
        return suspendCancellableCoroutine { continuation ->
            billingClient.consumeAsync(params) { billingResult, _ ->
                continuation.resume(billingResult)
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        purchaseUpdatesFlow.tryEmit(PurchasesUpdatedEvent(billingResult, purchases))
    }

    private companion object {
        const val PURCHASE_UPDATES_BUFFER = 8
    }
}
