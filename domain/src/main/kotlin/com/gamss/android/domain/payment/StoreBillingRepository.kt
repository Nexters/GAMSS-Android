package com.gamss.android.domain.payment

import kotlinx.coroutines.flow.StateFlow

enum class StoreConnectionState { DISCONNECTED, CONNECTING, CONNECTED }

/** 스토어 구현이 결제 직전 조회한 상품 객체를 보존한다. */
interface StoreProduct {
    val productId: String
    val formattedPrice: String
}

data class StoreProductQueryResult(
    val isSuccess: Boolean,
    val products: List<StoreProduct>,
)

interface StoreBillingRepository {
    val connectionState: StateFlow<StoreConnectionState>
    fun connect()
    suspend fun getProducts(productIds: List<String>): StoreProductQueryResult
}
