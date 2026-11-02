package com.gamss.android.domain.billing

/**
 * Play 의 (BillingResponseCode, PurchaseState) 조합을 정규화한 결과. PurchasesUpdatedListener
 * 콜백과 미처리 구매 복구 조회(queryPurchasesAsync) 양쪽 모두 이 타입으로 변환된 뒤
 * 동일한 처리 경로로 들어간다 — 호출 출처에 따라 분기가 갈리지 않는다.
 */
sealed interface PurchaseOutcome {
    /** OK + PURCHASED. */
    data class Confirmed(val productId: String, val purchaseToken: String) : PurchaseOutcome

    /** OK + PENDING. 토큰을 지급하지 않는다. */
    data object Pending : PurchaseOutcome

    /** USER_CANCELED. 오류 안내 없이 구매 중 상태만 해제한다. */
    data object UserCanceled : PurchaseOutcome

    /** ITEM_ALREADY_OWNED. 미처리 구매를 다시 조회해 복구한다. */
    data object AlreadyOwned : PurchaseOutcome

    /** 그 외 모든 응답 코드. 로깅·안내 대상이며 토큰을 지급하지 않는다. */
    data class Failed(val message: String?) : PurchaseOutcome
}
