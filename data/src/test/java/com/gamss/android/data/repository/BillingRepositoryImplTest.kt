package com.gamss.android.data.repository

import com.gamss.android.core.common.AppResult
import com.gamss.android.data.remote.billing.BillingService
import com.gamss.android.data.remote.billing.model.request.VerifyPurchaseRequest
import com.gamss.android.data.remote.billing.model.response.BillingProductResponse
import com.gamss.android.data.remote.billing.model.response.VerifyPurchaseResponse
import com.gamss.android.data.remote.model.response.ApiResponse
import com.gamss.android.domain.billing.GrantResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BillingRepositoryImplTest {

    private val billingService: BillingService = mockk()
    private val repository = BillingRepositoryImpl(billingService)

    @Test
    fun `서버 상품 목록을 도메인 모델로 변환한다`() = runTest {
        coEvery { billingService.getProducts() } returns ApiResponse(
            success = true,
            data = listOf(BillingProductResponse(productId = "pack_10", benefitDescription = "토큰 10개")),
        )

        val result = repository.getServerProducts()

        val products = (result as AppResult.Success).data
        assertEquals(1, products.size)
        assertEquals("pack_10", products[0].productId)
        assertEquals("토큰 10개", products[0].benefitDescription)
    }

    @Test
    fun `구매 검증이 지급 확정을 내려주면 Granted 를 반환한다`() = runTest {
        coEvery { billingService.verifyPurchase(any()) } returns ApiResponse(
            success = true,
            data = VerifyPurchaseResponse(alreadyGranted = false),
        )

        val result = repository.verifyAndGrantPurchase(productId = "pack_10", purchaseToken = "token-1")

        assertEquals(GrantResult.Granted, (result as AppResult.Success).data)
        coVerify {
            billingService.verifyPurchase(VerifyPurchaseRequest(productId = "pack_10", purchaseToken = "token-1"))
        }
    }

    @Test
    fun `이미 지급된 구매는 AlreadyGranted 를 반환한다 - 재조회를 막지 않는다`() = runTest {
        coEvery { billingService.verifyPurchase(any()) } returns ApiResponse(
            success = true,
            data = VerifyPurchaseResponse(alreadyGranted = true),
        )

        val result = repository.verifyAndGrantPurchase(productId = "pack_10", purchaseToken = "token-1")

        assertEquals(GrantResult.AlreadyGranted, (result as AppResult.Success).data)
    }

    @Test
    fun `서버가 success false 를 내려주면 실패로 처리한다`() = runTest {
        coEvery { billingService.getProducts() } returns ApiResponse(success = false, data = null)

        val result = repository.getServerProducts()

        assertTrue(result is AppResult.Failure)
    }
}
