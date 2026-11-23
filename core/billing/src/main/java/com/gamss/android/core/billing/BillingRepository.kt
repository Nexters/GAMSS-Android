package com.gamss.android.core.billing

import android.app.Activity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * 앱 전체가 공유하는 활성 BillingClient 연결 하나를 감싼다. 구매 처리를 특정 화면에 묶지 않도록
 * 싱글턴으로 주입되어, 구매 화면·한도 소진 알림 등 여러 진입점이 동일한 연결·구매 콜백 경로를
 * 공유한다.
 */
interface BillingRepository {
    val connectionState: StateFlow<BillingConnectionState>

    /**
     * 구매 콜백([com.android.billingclient.api.PurchasesUpdatedListener])과 미처리 구매 복구
     * 조회([queryUnfinishedPurchases])가 공유하는 단일 처리 경로. 각 결과는 정확히 한 번만
     * 전달된다 — 동시에 여러 곳에서 collect하면 결과가 그중 한 곳에만 전달되고 나눠진다. 지금은
     * 호출부가 한 곳에서만 collect한다는 전제로 쓴다.
     */
    val purchaseResults: Flow<BillingPurchaseOutcome>

    /** 이미 연결됐거나 연결 중이면 아무것도 하지 않는다 — 중복 연결 금지. */
    fun connect()

    /**
     * 매 호출마다 Play에 새로 조회한다 — 결과를 멤버 변수 등에 캐시하지 않는다. 오래된
     * ProductDetails로 [launchPurchaseFlow]를 호출하면 실패하기 때문이다.
     */
    suspend fun queryProductDetails(productIds: List<String>): BillingProductQueryResult

    /**
     * 메인 스레드에서 호출해야 한다. 즉시 반환되는 결과는 결제창 실행 성공/실패일 뿐,
     * 결제 완료 여부가 아니다 — 실제 결과는 [purchaseResults]로 들어온다.
     */
    fun launchPurchaseFlow(
        activity: Activity,
        product: BillingProduct,
        obfuscatedAccountId: String,
    ): BillingOperationResult

    /**
     * INAPP 미처리 구매를 다시 조회한다. 결과는 구매 콜백과 동일한 [purchaseResults]로 흘러간다.
     * 앱 시작, 구매 화면 진입, 포그라운드 복귀 시 호출부가 불러야 한다. Billing 연결 성공 시에는
     * [connect]가 자체적으로 호출하므로 호출부가 별도로 부를 필요는 없다 — 중복 호출하면 같은
     * 미처리 구매가 [purchaseResults]로 두 번 전달될 수 있다.
     */
    suspend fun queryUnfinishedPurchases()

    /**
     * 서버 지급 응답을 받은 뒤에만 호출해야 한다 — [BillingPurchaseOutcome.Purchased]를 받자마자
     * 호출하면 서버 검증·지급 전에 구매가 소비되어, 결제는 됐는데 지급은 안 되는 경로가 생길 수 있다.
     */
    suspend fun consumePurchase(purchaseToken: String): BillingOperationResult
}
