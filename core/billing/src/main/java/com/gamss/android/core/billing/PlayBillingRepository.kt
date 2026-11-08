package com.gamss.android.core.billing

import android.app.Activity
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingResult
import com.gamss.android.core.billing.di.BillingCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [PlayBillingDataSource]가 콜백 기반 BillingClient API를 Flow로 감싸 넘겨주면, 여기서는 SDK를
 * 전혀 모른 채 도메인 판단(연결 중복 방지, 응답코드→[BillingPurchaseOutcome] 매핑, 미처리 구매
 * 복구 트리거)만 담당한다.
 */
@Singleton
internal class PlayBillingRepository @Inject constructor(
    private val dataSource: PlayBillingDataSource,
    @BillingCoroutineScope private val scope: CoroutineScope,
) : BillingRepository {

    private val _connectionState = MutableStateFlow(BillingConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<BillingConnectionState> = _connectionState.asStateFlow()

    private val _purchaseResults =
        MutableSharedFlow<BillingPurchaseOutcome>(extraBufferCapacity = PURCHASE_RESULTS_BUFFER)
    override val purchaseResults: Flow<BillingPurchaseOutcome> = _purchaseResults.asSharedFlow()

    init {
        scope.launch {
            dataSource.connectionEvents.collect { event -> handleConnectionEvent(event) }
        }
        scope.launch {
            dataSource.purchaseUpdates.collect { event ->
                emitOutcomes(mapToPurchaseOutcomes(event.billingResult, event.purchases))
            }
        }
    }

    override fun connect() {
        if (_connectionState.value == BillingConnectionState.CONNECTED ||
            _connectionState.value == BillingConnectionState.CONNECTING
        ) {
            return
        }
        _connectionState.value = BillingConnectionState.CONNECTING
        dataSource.startConnection()
    }

    override suspend fun queryProductDetails(productIds: List<String>): BillingProductQueryResult {
        val (billingResult, result) = dataSource.queryProductDetails(productIds)

        val products = result.productDetailsList.mapNotNull { productDetails ->
            val offer = productDetails.oneTimePurchaseOfferDetailsList?.firstOrNull() ?: return@mapNotNull null
            BillingProduct(
                productDetails = productDetails,
                productId = productDetails.productId,
                offerToken = offer.offerToken,
                formattedPrice = offer.formattedPrice,
            )
        }
        return BillingProductQueryResult(
            callResult = billingResult.toOperationResult(),
            products = products,
            unfetchedProductIds = result.unfetchedProductList.map { it.productId },
        )
    }

    override fun launchPurchaseFlow(
        activity: Activity,
        product: BillingProduct,
        obfuscatedAccountId: String,
    ): BillingOperationResult =
        dataSource.launchBillingFlow(activity, product, obfuscatedAccountId).toOperationResult()

    override suspend fun queryUnfinishedPurchases() {
        val (billingResult, purchases) = dataSource.queryPurchases()
        emitOutcomes(mapToPurchaseOutcomes(billingResult, purchases))
    }

    override suspend fun consumePurchase(purchaseToken: String): BillingOperationResult =
        dataSource.consumePurchase(purchaseToken).billingResult.toOperationResult()

    private fun BillingResult.toOperationResult() = BillingOperationResult(
        isSuccess = responseCode == BillingResponseCode.OK,
        responseCode = responseCode,
    )

    private fun handleConnectionEvent(event: BillingConnectionEvent) {
        when (event) {
            is BillingConnectionEvent.SetupFinished -> {
                if (event.billingResult.responseCode == BillingResponseCode.OK) {
                    _connectionState.value = BillingConnectionState.CONNECTED
                    scope.launch { queryUnfinishedPurchases() }
                } else {
                    _connectionState.value = BillingConnectionState.DISCONNECTED
                }
            }
            // 자동 재연결은 호출부가 책임진다(앱 시작·화면 진입·포그라운드 복귀 시 connect() 재호출).
            BillingConnectionEvent.ServiceDisconnected -> _connectionState.value = BillingConnectionState.DISCONNECTED
        }
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
