package com.gamss.android.app.payment

import android.app.Activity
import com.gamss.android.core.billing.BillingConnectionState
import com.gamss.android.core.billing.BillingOperationResult
import com.gamss.android.core.billing.BillingProduct
import com.gamss.android.core.billing.BillingProductQueryResult
import com.gamss.android.core.billing.BillingRepository
import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.payment.GetPaymentAccountTokenUseCase
import com.gamss.android.domain.payment.GetPaymentProductsUseCase
import com.gamss.android.domain.payment.TokenProduct
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.orbitmvi.orbit.test.test

@OptIn(ExperimentalCoroutinesApi::class)
class PurchaseViewModelTest {

    private val getPaymentProducts: GetPaymentProductsUseCase = mockk()
    private val getPaymentAccountToken: GetPaymentAccountTokenUseCase = mockk()
    private val billing: BillingRepository = mockk()
    private val verifier: PendingPurchaseVerifier = mockk()

    init {
        every { verifier.state } returns MutableStateFlow(PaymentState())
    }

    private fun viewModel(
        connectionState: MutableStateFlow<BillingConnectionState> = MutableStateFlow(BillingConnectionState.DISCONNECTED),
    ): PurchaseViewModel {
        every { billing.connectionState } returns connectionState
        return PurchaseViewModel(getPaymentProducts, getPaymentAccountToken, billing, verifier)
    }

    @Test
    fun `Play 연결이 완료되면 상품 목록을 가격과 함께 표시한다`() = runTest {
        coEvery { getPaymentProducts() } returns AppResult.Success(
            listOf(TokenProduct(productId = "token_300000", tokenAmount = 300_000)),
        )
        val billingProduct = billingProduct("token_300k")
        coEvery { billing.queryProductDetails(listOf("token_300k")) } returns BillingProductQueryResult(
            callResult = BillingOperationResult(isSuccess = true, responseCode = 0),
            products = listOf(billingProduct),
            unfetchedProductIds = emptyList(),
        )
        val connectionState = MutableStateFlow(BillingConnectionState.DISCONNECTED)

        viewModel(connectionState).test(this) {
            connectionState.value = BillingConnectionState.CONNECTED

            expectState { copy(isLoading = true, error = null) }
            expectState {
                copy(
                    isLoading = false,
                    products = listOf(PurchaseProduct(TokenProduct("token_300k", 300_000), billingProduct)),
                    error = null,
                )
            }
        }
    }

    @Test
    fun `서버 상품 조회가 실패하면 에러 메시지를 표시한다`() = runTest {
        coEvery { getPaymentProducts() } returns AppResult.Failure(IllegalStateException())
        val connectionState = MutableStateFlow(BillingConnectionState.DISCONNECTED)

        viewModel(connectionState).test(this) {
            connectionState.value = BillingConnectionState.CONNECTED

            expectState { copy(isLoading = true, error = null) }
            expectState { copy(isLoading = false, error = "구매 정보를 불러오지 못했습니다. 잠시 후 다시 확인해 주세요.") }
        }
    }

    @Test
    fun `판매 중인 상품이 없으면 Play 조회를 하지 않고 안내만 표시한다`() = runTest {
        coEvery { getPaymentProducts() } returns AppResult.Success(emptyList())
        val connectionState = MutableStateFlow(BillingConnectionState.DISCONNECTED)

        viewModel(connectionState).test(this) {
            connectionState.value = BillingConnectionState.CONNECTED

            expectState { copy(isLoading = true, error = null) }
            expectState { copy(isLoading = false, products = emptyList(), error = "판매 중인 상품이 없습니다.") }
        }
        coVerify(exactly = 0) { billing.queryProductDetails(any()) }
    }

    @Test
    fun `Play 상품 조회가 실패하면 에러 메시지를 표시한다`() = runTest {
        coEvery { getPaymentProducts() } returns AppResult.Success(
            listOf(TokenProduct(productId = "token_300000", tokenAmount = 300_000)),
        )
        coEvery { billing.queryProductDetails(listOf("token_300k")) } returns BillingProductQueryResult(
            callResult = BillingOperationResult(isSuccess = false, responseCode = 6),
            products = emptyList(),
            unfetchedProductIds = listOf("token_300k"),
        )
        val connectionState = MutableStateFlow(BillingConnectionState.DISCONNECTED)

        viewModel(connectionState).test(this) {
            connectionState.value = BillingConnectionState.CONNECTED

            expectState { copy(isLoading = true, error = null) }
            expectState {
                copy(
                    isLoading = false,
                    products = listOf(PurchaseProduct(TokenProduct("token_300k", 300_000), null)),
                    error = "Google Play 상품 조회에 실패했습니다.",
                )
            }
        }
    }

    @Test
    fun `계정 토큰 조회가 실패하면 결제창을 열지 않고 에러 메시지를 표시한다`() = runTest {
        coEvery { getPaymentAccountToken() } returns AppResult.Failure(IllegalStateException())

        viewModel().test(this) {
            containerHost.purchase(mockk<Activity>(relaxed = true), "token_300k")

            expectState { copy(isPreparing = true, error = null) }
            expectState { copy(isPreparing = false, error = "구매 정보를 불러오지 못했습니다. 잠시 후 다시 확인해 주세요.") }
        }
        coVerify(exactly = 0) { billing.queryProductDetails(any()) }
    }

    @Test
    fun `구매 직전 상품 정보를 찾지 못하면 결제창을 열지 않는다`() = runTest {
        coEvery { getPaymentAccountToken() } returns AppResult.Success("account-token")
        coEvery { billing.queryProductDetails(listOf("token_300k")) } returns BillingProductQueryResult(
            callResult = BillingOperationResult(isSuccess = true, responseCode = 0),
            products = emptyList(),
            unfetchedProductIds = listOf("token_300k"),
        )

        viewModel().test(this) {
            containerHost.purchase(mockk<Activity>(relaxed = true), "token_300k")

            expectState { copy(isPreparing = true, error = null) }
            expectState { copy(isPreparing = false, error = "구매 가능한 상품 정보를 찾지 못했습니다.") }
        }
        coVerify(exactly = 0) { billing.launchPurchaseFlow(any(), any(), any()) }
    }

    @Test
    fun `결제창 실행에 실패하면 에러 메시지를 표시한다`() = runTest {
        val product = billingProduct("token_300k")
        coEvery { getPaymentAccountToken() } returns AppResult.Success("account-token")
        coEvery { billing.queryProductDetails(listOf("token_300k")) } returns BillingProductQueryResult(
            callResult = BillingOperationResult(isSuccess = true, responseCode = 0),
            products = listOf(product),
            unfetchedProductIds = emptyList(),
        )
        every {
            billing.launchPurchaseFlow(any(), product, "account-token")
        } returns BillingOperationResult(isSuccess = false, responseCode = 6)
        val activity = mockk<Activity>(relaxed = true) {
            every { isFinishing } returns false
            every { isDestroyed } returns false
        }

        viewModel().test(this) {
            containerHost.purchase(activity, "token_300k")

            expectState { copy(isPreparing = true, error = null) }
            expectState { copy(isPreparing = false, error = "결제창을 열 수 없습니다. (코드: 6)") }
        }
    }

    @Test
    fun `결제창 실행에 성공하면 에러를 지운다`() = runTest {
        val product = billingProduct("token_300k")
        coEvery { getPaymentAccountToken() } returns AppResult.Success("account-token")
        coEvery { billing.queryProductDetails(listOf("token_300k")) } returns BillingProductQueryResult(
            callResult = BillingOperationResult(isSuccess = true, responseCode = 0),
            products = listOf(product),
            unfetchedProductIds = emptyList(),
        )
        every {
            billing.launchPurchaseFlow(any(), product, "account-token")
        } returns BillingOperationResult(isSuccess = true, responseCode = 0)
        val activity = mockk<Activity>(relaxed = true) {
            every { isFinishing } returns false
            every { isDestroyed } returns false
        }

        viewModel().test(this) {
            containerHost.purchase(activity, "token_300k")

            expectState { copy(isPreparing = true, error = null) }
            expectState { copy(isPreparing = false, error = null) }
        }
    }

    @Test
    fun `액티비티가 종료 중이면 결제창을 열지 않는다`() = runTest {
        val product = billingProduct("token_300k")
        coEvery { getPaymentAccountToken() } returns AppResult.Success("account-token")
        coEvery { billing.queryProductDetails(listOf("token_300k")) } returns BillingProductQueryResult(
            callResult = BillingOperationResult(isSuccess = true, responseCode = 0),
            products = listOf(product),
            unfetchedProductIds = emptyList(),
        )
        val activity = mockk<Activity>(relaxed = true) {
            every { isFinishing } returns true
        }

        viewModel().test(this) {
            containerHost.purchase(activity, "token_300k")

            expectState { copy(isPreparing = true, error = null) }
            expectState { copy(isPreparing = false) }
        }
        coVerify(exactly = 0) { billing.launchPurchaseFlow(any(), any(), any()) }
    }

    private fun billingProduct(productId: String): BillingProduct = mockk {
        every { this@mockk.productId } returns productId
    }
}
