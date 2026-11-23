package com.gamss.android.app.payment

import com.gamss.android.domain.payment.StoreConnectionState
import com.gamss.android.core.billing.BillingOperationResult
import com.gamss.android.domain.payment.StoreProduct
import com.gamss.android.domain.payment.StoreProductQueryResult
import com.gamss.android.domain.payment.ConnectBillingUseCase
import com.gamss.android.domain.payment.GetBillingProductsUseCase
import com.gamss.android.domain.payment.ObserveBillingConnectionUseCase
import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.payment.GetPaymentAccountTokenUseCase
import com.gamss.android.domain.payment.GetPaymentProductsUseCase
import com.gamss.android.domain.payment.TokenProduct
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import org.junit.Test
import org.orbitmvi.orbit.test.test

@OptIn(ExperimentalCoroutinesApi::class)
class PurchaseViewModelTest {

    private val getPaymentProducts: GetPaymentProductsUseCase = mockk()
    private val getPaymentAccountToken: GetPaymentAccountTokenUseCase = mockk()
    private val observeBillingConnection: ObserveBillingConnectionUseCase = mockk()
    private val connectBilling: ConnectBillingUseCase = mockk(relaxed = true)
    private val getBillingProducts: GetBillingProductsUseCase = mockk()
    private val observePaymentState: ObservePaymentStateUseCase = mockk()
    private val requestPaymentRecovery: RequestPaymentRecoveryUseCase = mockk(relaxed = true)
    private val paymentState = MutableStateFlow(PaymentState())

    init {
        every { observePaymentState() } returns paymentState
    }

    @Test
    fun `구매 검증 상태를 Orbit 화면 상태에 반영한다`() = runTest {
        viewModel().test(this) {
            containerHost.requestRecovery()
            runCurrent()
            paymentState.value = PaymentState(isVerifying = true, message = "구매를 검증하고 있습니다.")
            expectState { copy(payment = PaymentState(isVerifying = true, message = "구매를 검증하고 있습니다.")) }
            containerHost.stopObservingPayment()
        }
    }

    private fun viewModel(
        connectionState: MutableStateFlow<StoreConnectionState> = MutableStateFlow(StoreConnectionState.DISCONNECTED),
    ): PurchaseViewModel {
        every { observeBillingConnection() } returns connectionState
        return PurchaseViewModel(
            getPaymentProducts, getPaymentAccountToken, observeBillingConnection, connectBilling, getBillingProducts,
            observePaymentState, requestPaymentRecovery,
        )
    }

    @Test
    fun `연결되지 않은 상태에서 새로고침하면 Billing 연결을 요청한다`() = runTest {
        viewModel().test(this) {
            containerHost.loadProducts()
            runCurrent()
            verify(exactly = 1) { connectBilling() }
            coVerify(exactly = 0) { getPaymentProducts() }
        }
    }

    @Test
    fun `Play 연결이 완료되면 상품 목록을 가격과 함께 표시한다`() = runTest {
        coEvery { getPaymentProducts() } returns AppResult.Success(
            listOf(TokenProduct(productId = "token_300000", tokenAmount = 300_000)),
        )
        val billingProduct = billingProduct("token_300000")
        coEvery { getBillingProducts(listOf("token_300000")) } returns StoreProductQueryResult(
            isSuccess = true,
            products = listOf(billingProduct),
        )
        val connectionState = MutableStateFlow(StoreConnectionState.DISCONNECTED)

        viewModel(connectionState).test(this) {
            connectionState.value = StoreConnectionState.CONNECTED

            expectState { copy(isLoading = true, error = null) }
            expectState {
                copy(
                    isLoading = false,
                    products = listOf(PurchaseProduct(TokenProduct("token_300000", 300_000), billingProduct)),
                    error = null,
                )
            }
        }
    }

    @Test
    fun `서버 상품 조회가 실패하면 에러 메시지를 표시한다`() = runTest {
        coEvery { getPaymentProducts() } returns AppResult.Failure(IllegalStateException())
        val connectionState = MutableStateFlow(StoreConnectionState.DISCONNECTED)

        viewModel(connectionState).test(this) {
            connectionState.value = StoreConnectionState.CONNECTED

            expectState { copy(isLoading = true, error = null) }
            expectState { copy(isLoading = false, error = "구매 정보를 불러오지 못했습니다. 잠시 후 다시 확인해 주세요.") }
        }
    }

    @Test
    fun `판매 중인 상품이 없으면 Play 조회를 하지 않고 안내만 표시한다`() = runTest {
        coEvery { getPaymentProducts() } returns AppResult.Success(emptyList())
        val connectionState = MutableStateFlow(StoreConnectionState.DISCONNECTED)

        viewModel(connectionState).test(this) {
            connectionState.value = StoreConnectionState.CONNECTED

            expectState { copy(isLoading = true, error = null) }
            expectState { copy(isLoading = false, products = emptyList(), error = "판매 중인 상품이 없습니다.") }
        }
        coVerify(exactly = 0) { getBillingProducts(any()) }
    }

    @Test
    fun `Play 상품 조회가 실패하면 에러 메시지를 표시한다`() = runTest {
        coEvery { getPaymentProducts() } returns AppResult.Success(
            listOf(TokenProduct(productId = "token_300000", tokenAmount = 300_000)),
        )
        coEvery { getBillingProducts(listOf("token_300000")) } returns StoreProductQueryResult(
            isSuccess = false,
            products = emptyList(),
        )
        val connectionState = MutableStateFlow(StoreConnectionState.DISCONNECTED)

        viewModel(connectionState).test(this) {
            connectionState.value = StoreConnectionState.CONNECTED

            expectState { copy(isLoading = true, error = null) }
            expectState {
                copy(
                    isLoading = false,
                    products = listOf(PurchaseProduct(TokenProduct("token_300000", 300_000), null)),
                    error = "Google Play 상품 조회에 실패했습니다.",
                )
            }
        }
    }

    @Test
    fun `계정 토큰 조회가 실패하면 결제창을 열지 않고 에러 메시지를 표시한다`() = runTest {
        coEvery { getPaymentAccountToken() } returns AppResult.Failure(IllegalStateException())

        viewModel().test(this) {
            containerHost.purchase("token_300000")

            expectState { copy(isPreparing = true, error = null) }
            expectState { copy(isPreparing = false, error = "구매 정보를 불러오지 못했습니다. 잠시 후 다시 확인해 주세요.") }
        }
        coVerify(exactly = 0) { getBillingProducts(any()) }
    }

    @Test
    fun `구매 직전 상품 정보를 찾지 못하면 결제창을 열지 않는다`() = runTest {
        coEvery { getPaymentAccountToken() } returns AppResult.Success("account-token")
        coEvery { getBillingProducts(listOf("token_300000")) } returns StoreProductQueryResult(
            isSuccess = true,
            products = emptyList(),
        )

        viewModel().test(this) {
            containerHost.purchase("token_300000")

            expectState { copy(isPreparing = true, error = null) }
            expectState { copy(isPreparing = false, error = "구매 가능한 상품 정보를 찾지 못했습니다.") }
        }
    }

    @Test
    fun `결제창 실행 요청을 전달하고 실패 결과를 표시한다`() = runTest {
        val product = billingProduct("token_300000")
        coEvery { getPaymentAccountToken() } returns AppResult.Success("account-token")
        coEvery { getBillingProducts(listOf("token_300000")) } returns StoreProductQueryResult(
            isSuccess = true,
            products = listOf(product),
        )

        viewModel().test(this) {
            containerHost.purchase("token_300000")
            expectState { copy(isPreparing = true, error = null) }
            expectSideEffect(PurchaseSideEffect.LaunchBillingFlow(product, "account-token"))
            containerHost.onBillingFlowResult(BillingOperationResult(isSuccess = false, responseCode = 6))
            expectState { copy(isPreparing = false, error = "결제창을 열 수 없습니다. (코드: 6)") }
        }
    }

    private fun billingProduct(productId: String): StoreProduct = mockk {
        every { this@mockk.productId } returns productId
    }
}
