package com.gamss.android.core.billing

import android.app.Activity
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeResult
import com.android.billingclient.api.PurchasesResult
import com.android.billingclient.api.QueryProductDetailsResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * [PlayBillingDataSource]의 테스트 더블. 실제 BillingClient의 콜백 시그니처를 몰라도
 * [emitConnectionEvent]/[emitPurchaseUpdate]로 임의 시점에 연결 끊김·구매 콜백 등을 주입해
 * 시나리오를 구성할 수 있다.
 */
internal class FakeBillingDataSource : PlayBillingDataSource {

    private val connectionEventsFlow = MutableSharedFlow<BillingConnectionEvent>(extraBufferCapacity = 1)
    private val purchaseUpdatesFlow = MutableSharedFlow<PurchasesUpdatedEvent>(extraBufferCapacity = 8)

    override val connectionEvents: Flow<BillingConnectionEvent> = connectionEventsFlow.asSharedFlow()
    override val purchaseUpdates: Flow<PurchasesUpdatedEvent> = purchaseUpdatesFlow.asSharedFlow()

    /** startConnection()이 호출될 때 실제 SDK가 동기적으로 콜백을 주는 상황을 흉내내고 싶을 때 쓴다. */
    var onStartConnection: () -> Unit = {}

    var startConnectionCallCount = 0
        private set
    var endConnectionCallCount = 0
        private set
    var queryProductDetailsCallCount = 0
        private set
    var queryPurchasesCallCount = 0
        private set
    var consumePurchaseCallCount = 0
        private set

    var queryProductDetailsResult: Pair<BillingResult, QueryProductDetailsResult> =
        okResult() to QueryProductDetailsResult.create(emptyList(), emptyList())
    var queryPurchasesResult: PurchasesResult = PurchasesResult(okResult(), emptyList())
    var consumePurchaseResult: ConsumeResult = ConsumeResult(okResult(), "")
    var launchBillingFlowResult: BillingResult = okResult()

    override fun startConnection() {
        startConnectionCallCount++
        onStartConnection()
    }

    override fun endConnection() {
        endConnectionCallCount++
    }

    override suspend fun queryProductDetails(productIds: List<String>): Pair<BillingResult, QueryProductDetailsResult> {
        queryProductDetailsCallCount++
        return queryProductDetailsResult
    }

    override fun launchBillingFlow(
        activity: Activity,
        product: BillingProduct,
        obfuscatedAccountId: String,
    ): BillingResult = launchBillingFlowResult

    override suspend fun queryPurchases(): PurchasesResult {
        queryPurchasesCallCount++
        return queryPurchasesResult
    }

    override suspend fun consumePurchase(purchaseToken: String): ConsumeResult {
        consumePurchaseCallCount++
        return consumePurchaseResult
    }

    fun emitConnectionEvent(event: BillingConnectionEvent) = connectionEventsFlow.tryEmit(event)

    fun emitPurchaseUpdate(event: PurchasesUpdatedEvent) = purchaseUpdatesFlow.tryEmit(event)

    companion object {
        fun okResult(): BillingResult = BillingResult.newBuilder()
            .setResponseCode(BillingResponseCode.OK)
            .build()
    }
}
