package com.gamss.android.feature.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/** PurchasesUpdatedListener 콜백을 그대로 흘려보낸다. 변환은 [PurchaseResultMapper]가 맡는다. */
data class PlayPurchaseUpdate(
    val billingResult: BillingResult,
    val purchases: List<Purchase>?,
)

/**
 * BillingClient 연결·조회·결제창 실행을 한 곳에 캡슐화한다. Application 생명주기 동안 하나만
 * 유지하며, 연결이 끊기면 다음 호출에서 다시 연결한다.
 */
@Singleton
class PlayBillingManager @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        _purchaseUpdates.tryEmit(PlayPurchaseUpdate(billingResult, purchases))
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private val _purchaseUpdates = MutableSharedFlow<PlayPurchaseUpdate>(extraBufferCapacity = PURCHASE_UPDATE_BUFFER)

    /** PurchasesUpdatedListener 콜백. 구매 버튼 클릭으로 연 결제창의 결과가 여기로 온다. */
    val purchaseUpdates: SharedFlow<PlayPurchaseUpdate> = _purchaseUpdates

    private suspend fun ensureConnected() {
        if (billingClient.isReady) return
        suspendCancellableCoroutine { continuation ->
            billingClient.startConnection(
                object : BillingClientStateListener {
                    override fun onBillingSetupFinished(billingResult: BillingResult) {
                        if (continuation.isActive) continuation.resume(Unit)
                    }

                    override fun onBillingServiceDisconnected() {
                        // 다음 호출의 ensureConnected()가 isReady == false 를 보고 재연결을 시도한다.
                    }
                },
            )
        }
    }

    suspend fun queryProductDetails(productIds: List<String>): List<ProductDetails> {
        if (productIds.isEmpty()) return emptyList()
        ensureConnected()

        val products = productIds.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder().setProductList(products).build()
        val result = billingClient.queryProductDetails(params)
        // unfetchedProductList 에 남은 상품은 조회 실패로 간주해 구매 불가 처리한다(domain 쪽에서
        // 서버 목록과 비교해 걸러냄).
        return result.productDetailsList.orEmpty()
    }

    /** ITEM_ALREADY_OWNED 복구, 또는 재진입 시 미처리 구매 확인에 쓴다. 한 건만 있다고 가정하지 않는다. */
    suspend fun queryOwnedPurchases(): List<Purchase> {
        ensureConnected()
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        return billingClient.queryPurchasesAsync(params).purchasesList
    }

    /**
     * 메인 스레드에서 호출해야 한다. 반환되는 [BillingResult]는 결제창이 뜨는 데 성공했는지만
     * 말해준다 — OK 여도 결제가 끝난 게 아니다. 실제 결과는 [purchaseUpdates]로 온다.
     */
    fun launchBillingFlow(
        activity: Activity,
        productDetails: ProductDetails,
        offerToken: String,
        obfuscatedAccountId: String,
    ): BillingResult {
        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .setOfferToken(offerToken)
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .setObfuscatedAccountId(obfuscatedAccountId)
            .build()
        return billingClient.launchBillingFlow(activity, flowParams)
    }
}

private const val PURCHASE_UPDATE_BUFFER = 4
