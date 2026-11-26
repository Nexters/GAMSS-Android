package com.gamss.android.data.repository

import com.gamss.android.core.common.AppResult
import com.gamss.android.core.common.network.ApiException
import com.gamss.android.data.remote.model.response.ApiResponse
import com.gamss.android.data.remote.payment.PaymentService
import com.gamss.android.data.remote.payment.model.request.GooglePurchaseRequest
import com.gamss.android.data.remote.payment.model.response.AccountTokenResponse
import com.gamss.android.data.remote.payment.model.response.PaymentGrantResponse
import com.gamss.android.data.remote.payment.model.response.TokenProductResponse
import com.gamss.android.domain.auth.SessionExpiredException
import com.gamss.android.domain.payment.PaymentStatus
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class PaymentRepositoryImplTest {

    private val paymentService: PaymentService = mockk()
    private val repository = PaymentRepositoryImpl(paymentService)

    @Test
    fun `판매 상품 목록을 도메인 모델로 변환한다`() = runTest {
        coEvery { paymentService.getProducts() } returns ApiResponse(
            success = true,
            data = listOf(
                TokenProductResponse(productId = "token_300k", tokenAmount = 300_000),
                TokenProductResponse(productId = "token_500k", tokenAmount = 500_000),
            ),
        )

        val result = repository.getProducts()

        val products = (result as AppResult.Success).data
        assertEquals(listOf("token_300k", "token_500k"), products.map { it.productId })
        assertEquals(listOf(300_000L, 500_000L), products.map { it.tokenAmount })
    }

    @Test
    fun `계정 토큰을 그대로 돌려준다`() = runTest {
        coEvery { paymentService.getAccountToken() } returns ApiResponse(
            success = true,
            data = AccountTokenResponse(accountToken = "3f2b8c1e-7a4d-4e6b-9c0a-1d2e3f4a5b6c"),
        )

        val result = repository.getAccountToken()

        assertEquals("3f2b8c1e-7a4d-4e6b-9c0a-1d2e3f4a5b6c", (result as AppResult.Success).data)
    }

    @Test
    fun `탈퇴한 회원의 계정 토큰 조회는 실패로 유지한다`() = runTest {
        coEvery { paymentService.getAccountToken() } throws httpException("WITHDRAWN_MEMBER")

        val result = repository.getAccountToken()

        val throwable = (result as AppResult.Failure).throwable
        assertTrue(throwable is ApiException.Http)
        assertEquals("WITHDRAWN_MEMBER", (throwable as ApiException.Http).code)
    }

    @Test
    fun `GRANTED 응답을 지급 완료로 변환한다`() = runTest {
        coEvery {
            paymentService.verifyGooglePurchase(GooglePurchaseRequest("token_300k", "receipt"))
        } returns ApiResponse(
            success = true,
            data = PaymentGrantResponse(
                status = "GRANTED",
                productId = "token_300k",
                grantedTokens = 300_000,
                paidBalance = 600_000,
            ),
        )

        val result = repository.verifyGooglePurchase("token_300k", "receipt")

        val grant = (result as AppResult.Success).data
        assertEquals(PaymentStatus.GRANTED, grant.status)
        assertEquals(300_000L, grant.grantedTokens)
        assertEquals(600_000L, grant.paidBalance)
    }

    @Test
    fun `PENDING 응답은 지급되지 않은 상태로 변환한다`() = runTest {
        coEvery { paymentService.verifyGooglePurchase(any()) } returns ApiResponse(
            success = true,
            data = PaymentGrantResponse(status = "PENDING", productId = "token_300k"),
        )

        val result = repository.verifyGooglePurchase("token_300k", "receipt")

        val grant = (result as AppResult.Success).data
        assertEquals(PaymentStatus.PENDING, grant.status)
        assertEquals(0L, grant.grantedTokens)
        assertEquals(0L, grant.paidBalance)
    }

    @Test
    fun `서버가 요청과 다른 상품을 돌려주면 실패로 처리한다`() = runTest {
        coEvery { paymentService.verifyGooglePurchase(any()) } returns ApiResponse(
            success = true,
            data = PaymentGrantResponse(status = "GRANTED", productId = "token_500k", grantedTokens = 500_000),
        )

        val result = repository.verifyGooglePurchase("token_300k", "receipt")

        assertTrue((result as AppResult.Failure).throwable is IllegalStateException)
    }

    @Test
    fun `구매 검증의 401 응답은 세션 만료로 변환한다`() = runTest {
        coEvery { paymentService.verifyGooglePurchase(any()) } throws httpException("EXPIRED_TOKEN", UNAUTHORIZED)

        val result = repository.verifyGooglePurchase("token_300k", "receipt")

        assertTrue((result as AppResult.Failure).throwable is SessionExpiredException)
    }

    @Test
    fun `구매 검증의 서버 실패 코드를 그대로 유지한다`() = runTest {
        val codes = listOf(
            "INVALID_INPUT",
            "PURCHASE_INVALID",
            "PURCHASE_PRODUCT_MISMATCH",
            "WITHDRAWN_MEMBER",
            "PURCHASE_ACCESS_DENIED",
            "PURCHASE_CANCELLED",
            "PURCHASE_QUANTITY_UNSUPPORTED",
            "PAYMENT_PRODUCT_UNAVAILABLE",
            "PAYMENT_STORE_UNAVAILABLE",
        )

        codes.forEach { code ->
            coEvery { paymentService.verifyGooglePurchase(any()) } throws httpException(code)

            val result = repository.verifyGooglePurchase("token_300k", "receipt")

            val throwable = (result as AppResult.Failure).throwable
            assertTrue("$code 는 ApiException.Http로 유지돼야 한다", throwable is ApiException.Http)
            assertEquals(code, (throwable as ApiException.Http).code)
        }
    }

    @Test
    fun `네트워크 오류는 네트워크 실패로 유지한다`() = runTest {
        val cause = IOException("offline")
        coEvery { paymentService.verifyGooglePurchase(any()) } throws cause

        val result = repository.verifyGooglePurchase("token_300k", "receipt")

        val throwable = (result as AppResult.Failure).throwable
        assertTrue(throwable is ApiException.Network)
        assertSame(cause, throwable.cause)
    }

    @Test(expected = CancellationException::class)
    fun `취소는 실패로 변환하지 않고 전파한다`() = runTest {
        coEvery { paymentService.verifyGooglePurchase(any()) } throws CancellationException()

        repository.verifyGooglePurchase("token_300k", "receipt")
    }

    private fun httpException(code: String, statusCode: Int = BAD_REQUEST): HttpException {
        val errorBody = """{"success":false,"error":{"code":"$code","message":"failed"}}"""
            .toResponseBody("application/json".toMediaType())
        return HttpException(
            Response.error<ApiResponse<PaymentGrantResponse>>(statusCode, errorBody),
        )
    }

    private companion object {
        const val BAD_REQUEST = 400
        const val UNAUTHORIZED = 401
    }
}
