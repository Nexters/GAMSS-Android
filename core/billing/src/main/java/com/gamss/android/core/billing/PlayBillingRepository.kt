package com.gamss.android.core.billing

import android.app.Activity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryProductDetailsResult
import com.android.billingclient.api.QueryPurchasesParams
import com.gamss.android.core.billing.di.BillingCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * 모든 비동기 Play 호출(상품 조회·미처리 구매 조회·consume)을 listener 기반 원본 API를 그대로
 * suspendCancellableCoroutine으로 감싸 쓴다 — billing-ktx 의 suspend 확장을 쓰지 않는다. 그래야
 * [BillingClient]를 생성자로 주입받는 그대로 mockk로 대체해, 콜백을 직접 호출시켜 테스트할 수 있다
 * (static/top-level 확장 함수는 mockk로 가로채기 까다롭다).
 */
@Singleton
internal class PlayBillingRepository @Inject constructor(
    private val billingClient: BillingClient,
    forwardingListener: ForwardingPurchasesUpdatedListener,
    @BillingCoroutineScope private val scope: CoroutineScope,
) : BillingRepository, PurchasesUpdatedListener {

    private val _connectionState = MutableStateFlow(BillingConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<BillingConnectionState> = _connectionState.asStateFlow()

    private val _purchaseResults =
        MutableSharedFlow<BillingPurchaseOutcome>(extraBufferCapacity = PURCHASE_RESULTS_BUFFER)
    override val purchaseResults: Flow<BillingPurchaseOutcome> = _purchaseResults.asSharedFlow()

    init {
        forwardingListener.delegate = this
    }

    override fun connect() {
        if (_connectionState.value == BillingConnectionState.CONNECTED ||
            _connectionState.value == BillingConnectionState.CONNECTING
        ) {
            return
        }
        _connectionState.value = BillingConnectionState.CONNECTING
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    _connectionState.value = BillingConnectionState.CONNECTED
                    scope.launch { queryUnfinishedPurchases() }
                } else {
                    _connectionState.value = BillingConnectionState.DISCONNECTED
                }
            }

            // 자동 재연결은 호출부가 책임진다(앱 시작·화면 진입·포그라운드 복귀 시 connect() 재호출).
            override fun onBillingServiceDisconnected() {
                _connectionState.value = BillingConnectionState.DISCONNECTED
            }
        })
    }

    override suspend fun queryProductDetails(productIds: List<String>): BillingProductQueryResult {
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

        // 매 호출마다 새로 조회한다 — 결과를 캐시해 두지 않는다.
        val (billingResult, result) =
            suspendCancellableCoroutine<Pair<BillingResult, QueryProductDetailsResult>> { continuation ->
                billingClient.queryProductDetailsAsync(params) { resultCode, queryResult ->
                    continuation.resume(resultCode to queryResult)
                }
            }

        val products = result.productDetailsList.mapNotNull { productDetails ->
            val offer = productDetails.oneTimePurchaseOfferDetailsList?.firstOrNull() ?: return@mapNotNull null
            BillingProduct(
                productDetails = productDetails,
                offerToken = offer.offerToken,
                formattedPrice = offer.formattedPrice,
            )
        }
        return BillingProductQueryResult(
            billingResult = billingResult,
            products = products,
            unfetchedProductIds = result.unfetchedProductList.map { it.productId },
        )
    }

    override fun launchPurchaseFlow(
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

    override suspend fun queryUnfinishedPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        val (billingResult, purchases) =
            suspendCancellableCoroutine<Pair<BillingResult, List<Purchase>>> { continuation ->
                billingClient.queryPurchasesAsync(params) { result, purchaseList ->
                    continuation.resume(result to purchaseList)
                }
            }
        emitOutcomes(mapToPurchaseOutcomes(billingResult, purchases))
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
        emitOutcomes(mapToPurchaseOutcomes(billingResult, purchases))
    }

    private fun emitOutcomes(outcomes: List<BillingPurchaseOutcome>) {
        outcomes.forEach { outcome ->
            _purchaseResults.tryEmit(outcome)
            // 미소비 구매가 하나뿐이라고 단정하지 않고, 복구 조회를 다시 돌려 전부 찾아낸다.
            if (outcome is BillingPurchaseOutcome.AlreadyOwned) {
                scope.launch { queryUnfinishedPurchases() }
            }
        }
    }

    private companion object {
        const val PURCHASE_RESULTS_BUFFER = 8
    }
}
