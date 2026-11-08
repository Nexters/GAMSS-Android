package com.gamss.android.core.billing

import android.app.Activity
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeResult
import com.android.billingclient.api.PurchasesResult
import com.android.billingclient.api.QueryProductDetailsResult
import kotlinx.coroutines.flow.Flow

/**
 * [com.android.billingclient.api.BillingClient] 호출만 담당하는 얇은 경계. 콜백 기반 SDK API를
 * suspend 함수/Flow로 감싸는 것 외의 도메인 판단(연결 중복 방지, 응답코드→[BillingPurchaseOutcome]
 * 매핑 등)은 하지 않는다 — 그건 [PlayBillingRepository]의 책임이다. 테스트에서는 이 인터페이스를
 * [FakeBillingDataSource]로 바꿔, 실제 BillingClient의 콜백 시그니처를 몰라도 임의 시점에
 * 연결 끊김·구매 콜백 등을 주입해 시나리오를 구성할 수 있다.
 */
internal interface PlayBillingDataSource {
    val connectionEvents: Flow<BillingConnectionEvent>
    val purchaseUpdates: Flow<PurchasesUpdatedEvent>

    fun startConnection()
    fun endConnection()

    /** 매 호출마다 Play에 새로 조회한다 — 결과를 캐시하지 않는다. */
    suspend fun queryProductDetails(productIds: List<String>): Pair<BillingResult, QueryProductDetailsResult>

    /** 메인 스레드에서 호출해야 한다. */
    fun launchBillingFlow(
        activity: Activity,
        product: BillingProduct,
        obfuscatedAccountId: String,
    ): BillingResult

    suspend fun queryPurchases(): PurchasesResult

    suspend fun consumePurchase(purchaseToken: String): ConsumeResult
}
