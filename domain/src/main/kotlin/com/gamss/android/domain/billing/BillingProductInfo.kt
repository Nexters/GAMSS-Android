package com.gamss.android.domain.billing

/**
 * 서버가 내려주는 상품 메타데이터. 가격은 포함하지 않는다 — 화면에 보여줄 가격은 항상 Play
 * ProductDetails 의 formattedPrice 를 그대로 쓰고, 원화 가격을 서버·클라이언트 어디에도
 * 하드코딩하지 않는다.
 */
data class BillingProductInfo(
    val productId: String,
    val benefitDescription: String,
)
