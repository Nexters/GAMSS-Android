package com.gamss.android.domain.payment

import kotlinx.coroutines.flow.StateFlow

/**
 * [FAILED]는 연결 설정 자체가 실패한 경우다(Play 스토어 미로그인·구버전 등). 연결된 뒤 끊긴 경우는 [DISCONNECTED]다.
 * 두 상태 모두 다시 연결을 시도할 수 있다.
 */
enum class StoreConnectionState { DISCONNECTED, CONNECTING, CONNECTED, FAILED }

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
