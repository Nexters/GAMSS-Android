package com.gamss.android.domain.billing

import com.gamss.android.core.common.AppResult

interface BillingRepository {

    /** 서버가 판매 중으로 등록한 상품 메타데이터 목록. 가격은 포함하지 않는다. */
    suspend fun getServerProducts(): AppResult<List<BillingProductInfo>>

    /**
     * purchaseToken 을 서버로 보내 구매를 검증하고 지급을 확정한다.
     * [GrantResult.AlreadyGranted] 도 실패가 아니라 성공 경로로 처리해야 한다.
     */
    suspend fun verifyAndGrantPurchase(
        productId: String,
        purchaseToken: String,
    ): AppResult<GrantResult>
}
