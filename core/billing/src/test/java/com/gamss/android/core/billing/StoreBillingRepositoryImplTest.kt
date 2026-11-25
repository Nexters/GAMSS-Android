package com.gamss.android.core.billing

import com.gamss.android.domain.payment.StoreConnectionState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StoreBillingRepositoryImplTest {
    @Test
    fun `연결 상태를 전달하고 조회한 상품 객체를 그대로 유지한다`() = runTest {
        val connection = MutableStateFlow(BillingConnectionState.DISCONNECTED)
        val product = mockk<BillingProduct>()
        val billing = mockk<BillingRepository>()
        every { billing.connectionState } returns connection
        coEvery { billing.queryProductDetails(listOf("p1")) } returns BillingProductQueryResult(
            BillingOperationResult(true, 0), listOf(product), emptyList(),
        )
        val repository = StoreBillingRepositoryImpl(billing)
        connection.value = BillingConnectionState.CONNECTED

        assertEquals(StoreConnectionState.CONNECTED, repository.connectionState.value)
        val result = repository.getProducts(listOf("p1"))
        assertEquals(true, result.isSuccess)
        assertSame(product, result.products.single())
    }
}
