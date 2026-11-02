package com.gamss.android.domain.billing

/**
 * 서버의 구매 검증·지급 응답. [AlreadyGranted] 도 [Granted] 와 동일하게 잔액 재조회를
 * 트리거해야 한다 — 멱등 응답이라고 화면 갱신을 건너뛰면 사용자가 최신 잔액을 못 본다.
 */
sealed interface GrantResult {
    data object Granted : GrantResult
    data object AlreadyGranted : GrantResult
}
