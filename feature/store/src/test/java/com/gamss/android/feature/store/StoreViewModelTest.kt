package com.gamss.android.feature.store

import com.gamss.android.core.billing.BillingOperationResult
import com.gamss.android.core.billing.PaymentEvent
import com.gamss.android.core.billing.PaymentState
import com.gamss.android.core.billing.PendingPurchaseVerifier
import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.payment.ConnectBillingUseCase
import com.gamss.android.domain.payment.GetBillingProductsUseCase
import com.gamss.android.domain.payment.GetPaymentAccountTokenUseCase
import com.gamss.android.domain.payment.GetPaymentProductsUseCase
import com.gamss.android.domain.payment.ObserveBillingConnectionUseCase
import com.gamss.android.domain.payment.StoreConnectionState
import com.gamss.android.domain.payment.StoreProduct
import com.gamss.android.domain.payment.StoreProductQueryResult
import com.gamss.android.domain.payment.TokenProduct
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.orbitmvi.orbit.test.test

@OptIn(ExperimentalCoroutinesApi::class)
class StoreViewModelTest {

    private val getPaymentProducts: GetPaymentProductsUseCase = mockk()
    private val getPaymentAccountToken: GetPaymentAccountTokenUseCase = mockk()
    private val observeBillingConnection: ObserveBillingConnectionUseCase = mockk()
    private val connectBilling: ConnectBillingUseCase = mockk(relaxed = true)
    private val getBillingProducts: GetBillingProductsUseCase = mockk()
    private val paymentVerifier: PendingPurchaseVerifier = mockk(relaxed = true)
    private val paymentState = MutableStateFlow(PaymentState())
    private val paymentEvents = MutableSharedFlow<PaymentEvent>(extraBufferCapacity = 1)

    init {
        every { paymentVerifier.state } returns paymentState
        every { paymentVerifier.events } returns paymentEvents
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
        isLicensedPackage: Boolean = true,
    ): StoreViewModel {
        every { observeBillingConnection() } returns connectionState
        return StoreViewModel(
            getPaymentProducts, getPaymentAccountToken, observeBillingConnection, connectBilling, getBillingProducts,
            paymentVerifier, isLicensedPackage,
        )
    }

    @Test
    fun `라이선스 테스트 패키지가 아니면 구매를 진행하지 않는다`() = runTest {
        viewModel(isLicensedPackage = false).test(this) {
            containerHost.purchase("token_300000")
            runCurrent()
            expectNoItems()
        }
        coVerify(exactly = 0) { getPaymentAccountToken() }
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
                    tokenProducts = listOf(
                        storeTokenProduct(TokenProduct("token_300000", 300_000), billingProduct),
                    ),
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
            expectState { copy(isLoading = false, tokenProducts = emptyList(), error = "판매 중인 상품이 없습니다.") }
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
                    tokenProducts = listOf(
                        storeTokenProduct(TokenProduct("token_300000", 300_000), null),
                    ),
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
            expectSideEffect(StoreSideEffect.LaunchBillingFlow(product, "account-token"))
            containerHost.onBillingFlowResult(BillingOperationResult(isSuccess = false, responseCode = 6))
            expectState { copy(isPreparing = false, error = "결제창을 열 수 없습니다. (코드: 6)") }
            expectSideEffect(StoreSideEffect.ShowToast(R.string.store_billing_launch_fail_toast))
        }
    }

    @Test
    fun `결제창 실행에 성공하면 구매 준비 상태만 해제하고 안내하지 않는다`() = runTest {
        val product = billingProduct("token_300000")
        coEvery { getPaymentAccountToken() } returns AppResult.Success("account-token")
        coEvery { getBillingProducts(listOf("token_300000")) } returns StoreProductQueryResult(
            isSuccess = true,
            products = listOf(product),
        )

        viewModel().test(this) {
            containerHost.purchase("token_300000")
            expectState { copy(isPreparing = true, error = null) }
            expectSideEffect(StoreSideEffect.LaunchBillingFlow(product, "account-token"))
            containerHost.onBillingFlowResult(BillingOperationResult(isSuccess = true, responseCode = 0))
            expectState { copy(isPreparing = false, error = null) }
            expectNoItems()
        }
    }

    @Test
    fun `서버 지급이 완료되면 구매 완료를 안내한다`() = runTest {
        viewModel().test(this) {
            containerHost.requestRecovery()
            runCurrent()
            paymentEvents.emit(PaymentEvent.Granted(grantedTokens = 300_000))
            expectSideEffect(StoreSideEffect.ShowToast(R.string.store_purchase_complete_toast))
            containerHost.stopObservingPayment()
        }
    }

    @Test
    fun `서버 검증에 실패하면 다시 시도하라고 안내한다`() = runTest {
        viewModel().test(this) {
            containerHost.requestRecovery()
            runCurrent()
            paymentEvents.emit(PaymentEvent.VerificationFailed)
            expectSideEffect(StoreSideEffect.ShowToast(R.string.store_purchase_verify_fail_toast))
            containerHost.stopObservingPayment()
        }
    }

    private fun billingProduct(productId: String): StoreProduct = mockk {
        every { this@mockk.productId } returns productId
    }

    private fun storeTokenProduct(product: TokenProduct, billingProduct: StoreProduct?): StoreTokenProduct {
        val visual = TokenProductVisuals.first()
        return StoreTokenProduct(
            product = product,
            billingProduct = billingProduct,
            labelRes = visual.labelRes,
            iconRes = visual.iconRes,
            iconSize = visual.iconSize,
        )
    }
}
