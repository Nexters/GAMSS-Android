package com.gamss.android.domain.payment

/** [paidBalance]는 응답 시점의 잔액이라 이전 응답과 다를 수 있다. */
data class PaymentGrant(
    val status: PaymentStatus,
    val productId: String,
    val grantedTokens: Long,
    val paidBalance: Long,
)
