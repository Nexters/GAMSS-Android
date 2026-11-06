package com.gamss.android.core.billing

import android.app.Activity
import com.android.billingclient.api.BillingResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * 앱 전체가 공유하는 활성 BillingClient 연결 하나를 감싼다. 구매 처리를 특정 화면에 묶지 않도록
 * 싱글턴으로 주입되어, 구매 화면·한도 소진 알림 등 여러 진입점이 동일한 연결·구매 콜백 경로를
 * 공유한다.
 */
interface BillingRepository {
    val connectionState: StateFlow<BillingConnectionState>

    /** 구매 콜백([com.android.billingclient.api.PurchasesUpdatedListener])과 미처리 구매 복구
     * 조회([queryUnfinishedPurchases])가 공유하는 단일 처리 경로. */
    val purchaseResults: Flow<BillingPurchaseOutcome>

    /** 이미 연결됐거나 연결 중이면 아무것도 하지 않는다 — 중복 연결 금지. */
    fun connect()

    /** 앱이 닫힐 때 호출해 연결을 종료한다. 이후 연결 상태는 [BillingConnectionState.CLOSED]가 된다. */
    fun endConnection()

    /**
     * 매 호출마다 Play에 새로 조회한다 — 결과를 멤버 변수 등에 캐시하지 않는다. 오래된
     * ProductDetails로 [launchPurchaseFlow]를 호출하면 실패하기 때문이다.
     */
    suspend fun queryProductDetails(productIds: List<String>): BillingProductQueryResult

    /**
     * 메인 스레드에서 호출해야 한다. 즉시 반환되는 [BillingResult]는 결제창 실행 성공/실패일 뿐,
     * 결제 완료 여부가 아니다 — 실제 결과는 [purchaseResults]로 들어온다.
     */
    fun launchPurchaseFlow(
        activity: Activity,
        product: BillingProduct,
        obfuscatedAccountId: String,
    ): BillingResult

    /**
     * INAPP 미처리 구매를 다시 조회한다. 결과는 구매 콜백과 동일한 [purchaseResults]로 흘러간다.
     * 앱 시작, 구매 화면 진입, 포그라운드 복귀, Billing 연결 성공 시 호출부가 불러야 한다.
     */
    suspend fun queryUnfinishedPurchases()

    suspend fun consumePurchase(purchaseToken: String): BillingResult
}
