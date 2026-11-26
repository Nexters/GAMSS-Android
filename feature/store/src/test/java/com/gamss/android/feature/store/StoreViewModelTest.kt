package com.gamss.android.feature.store

import android.util.Log
import com.gamss.android.core.billing.BillingOperationResult
import com.gamss.android.core.billing.BillingResultStatus
import com.gamss.android.core.billing.PaymentEvent
import com.gamss.android.core.billing.PaymentFailureReason
import com.gamss.android.core.billing.PaymentNotice
import com.gamss.android.core.billing.PaymentState
import com.gamss.android.core.billing.PendingPurchaseVerifier
import com.gamss.android.core.common.AppResult
import com.gamss.android.core.common.network.ApiException
import com.gamss.android.domain.auth.SessionExpiredException
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
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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

    init {
        every { paymentVerifier.state } returns paymentState
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
            getPaymentProducts,
            getPaymentAccountToken,
            observeBillingConnection,
            connectBilling,
            getBillingProducts,
            paymentVerifier,
            isLicensedPackage,
        )
    }

    @Test
    fun `라이선스 테스트 패키지가 아니면 화면 안내 없이 로그만 남기고 구매를 진행하지 않는다`() = runTest {
        // unit test 환경(android.jar 스텁)에서 Log.w가 "not mocked" 예외를 던지지 않도록 스텁 처리한다.
        mockkStatic(Log::class)
        every { Log.w(any<String>(), any<String>()) } returns 0
        try {
            viewModel(isLicensedPackage = false).test(this) {
                containerHost.purchase("token_300k")
                runCurrent()
                expectNoItems()
            }
            coVerify(exactly = 0) { getPaymentAccountToken() }
            verify(exactly = 1) { Log.w(any<String>(), any<String>()) }
        } finally {
            unmockkStatic(Log::class)
        }
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
            listOf(TokenProduct(productId = "token_300k", tokenAmount = 300_000)),
        )
        val billingProduct = billingProduct("token_300k")
        coEvery { getBillingProducts(listOf("token_300k")) } returns StoreProductQueryResult(
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
                        storeTokenProduct(TokenProduct("token_300k", 300_000), billingProduct),
                    ),
                    error = null,
                )
            }
        }
    }

    @Test
    fun `카드 라벨은 서버 지급량으로 만들고 아이콘은 지급량 순서대로 고른다`() = runTest {
        val serverProducts = listOf(
            TokenProduct(productId = "token_1m", tokenAmount = 1_000_000),
            TokenProduct(productId = "token_300k", tokenAmount = 300_000),
            TokenProduct(productId = "token_2m", tokenAmount = 2_000_000),
            TokenProduct(productId = "token_500k", tokenAmount = 500_000),
        )
        coEvery { getPaymentProducts() } returns AppResult.Success(serverProducts)
        coEvery { getBillingProducts(any()) } returns StoreProductQueryResult(isSuccess = true, products = emptyList())
        val connectionState = MutableStateFlow(StoreConnectionState.DISCONNECTED)

        viewModel(connectionState).test(this) {
            connectionState.value = StoreConnectionState.CONNECTED

            expectState { copy(isLoading = true, error = null) }
            val loaded = awaitState()
            assertEquals(
                listOf("token_300k", "token_500k", "token_1m", "token_2m"),
                loaded.tokenProducts.map { it.product.productId },
            )
            assertEquals(listOf(30L, 50L, 100L, 200L), loaded.tokenProducts.map { it.labelAmountInTenThousands })
            // 상품이 아이콘보다 많으면 마지막 아이콘을 반복한다.
            assertEquals(
                listOf(
                    R.drawable.img_store_coin_small,
                    R.drawable.img_store_coin_medium,
                    R.drawable.img_store_coin_large,
                    R.drawable.img_store_coin_large,
                ),
                loaded.tokenProducts.map { it.iconRes },
            )
        }
    }

    @Test
    fun `네트워크가 아닌 이유로 서버 상품 조회가 실패하면 상점 전용 재시도 안내를 표시한다`() = runTest {
        coEvery { getPaymentProducts() } returns AppResult.Failure(IllegalStateException())
        val connectionState = MutableStateFlow(StoreConnectionState.DISCONNECTED)

        viewModel(connectionState).test(this) {
            connectionState.value = StoreConnectionState.CONNECTED

            expectState { copy(isLoading = true) }
            expectState {
                copy(
                    isLoading = false,
                    loadFailure = StoreLoadFailure.UNKNOWN,
                    error = "구매 정보를 불러오지 못했습니다. 잠시 후 다시 확인해 주세요.",
                )
            }
        }
    }

    @Test
    fun `네트워크 오류로 서버 상품 조회가 실패하면 네트워크 오류 화면을 표시하고 재시도하면 다시 조회한다`() = runTest {
        coEvery { getPaymentProducts() } returnsMany listOf(
            AppResult.Failure(ApiException.Network(IllegalStateException())),
            AppResult.Success(emptyList()),
        )
        val connectionState = MutableStateFlow(StoreConnectionState.DISCONNECTED)

        viewModel(connectionState).test(this) {
            connectionState.value = StoreConnectionState.CONNECTED

            expectState { copy(isLoading = true) }
            val failed = awaitState()
            assertEquals(StoreLoadFailure.NETWORK, failed.loadFailure)

            containerHost.loadProducts()
            expectState { copy(isLoading = true, loadFailure = null, error = null) }
            expectState { copy(isLoading = false, isEmpty = true) }
        }
        coVerify(exactly = 2) { getPaymentProducts() }
    }

    @Test
    fun `판매 중인 상품이 없으면 Play 조회를 하지 않고 안내만 표시한다`() = runTest {
        coEvery { getPaymentProducts() } returns AppResult.Success(emptyList())
        val connectionState = MutableStateFlow(StoreConnectionState.DISCONNECTED)

        viewModel(connectionState).test(this) {
            connectionState.value = StoreConnectionState.CONNECTED

            expectState { copy(isLoading = true) }
            expectState { copy(isLoading = false, isEmpty = true) }
        }
        coVerify(exactly = 0) { getBillingProducts(any()) }
    }

    @Test
    fun `Play 상품 조회가 실패하면 에러 메시지를 표시한다`() = runTest {
        coEvery { getPaymentProducts() } returns AppResult.Success(
            listOf(TokenProduct(productId = "token_300k", tokenAmount = 300_000)),
        )
        coEvery { getBillingProducts(listOf("token_300k")) } returns StoreProductQueryResult(
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
                        storeTokenProduct(TokenProduct("token_300k", 300_000), null),
                    ),
                    error = "Google Play 상품 조회에 실패했습니다.",
                )
            }
            expectSideEffect(StoreSideEffect.ShowToast(R.string.store_product_info_unavailable_toast))
        }
    }

    @Test
    fun `계정 토큰 조회가 실패하면 결제창을 열지 않고 에러 메시지를 표시한다`() = runTest {
        coEvery { getPaymentAccountToken() } returns AppResult.Failure(IllegalStateException())

        viewModel().test(this) {
            containerHost.purchase("token_300k")

            expectState { copy(isPreparing = true, error = null) }
            expectState { copy(isPreparing = false, error = "구매 정보를 불러오지 못했습니다. 잠시 후 다시 확인해 주세요.") }
            expectSideEffect(StoreSideEffect.ShowToast(R.string.store_purchase_prepare_fail_toast))
        }
        coVerify(exactly = 0) { getBillingProducts(any()) }
    }

    @Test
    fun `계정 토큰 조회 실패 중 다시 시도로 해결되지 않는 원인은 원인을 안내한다`() = runTest {
        val expected = listOf(
            SessionExpiredException() to R.string.store_purchase_login_required_toast,
            ApiException.Http(403, "WITHDRAWN_MEMBER", "") to R.string.store_purchase_withdrawn_member_toast,
            ApiException.Network(IllegalStateException()) to R.string.store_purchase_prepare_fail_toast,
        )
        expected.forEach { (throwable, messageRes) ->
            coEvery { getPaymentAccountToken() } returns AppResult.Failure(throwable)
            viewModel().test(this) {
                containerHost.purchase("token_300k")
                expectState { copy(isPreparing = true, error = null) }
                awaitState()
                expectSideEffect(StoreSideEffect.ShowToast(messageRes))
            }
        }
    }

    @Test
    fun `구매 직전 상품 정보를 찾지 못하면 결제창을 열지 않는다`() = runTest {
        coEvery { getPaymentAccountToken() } returns AppResult.Success("account-token")
        coEvery { getBillingProducts(listOf("token_300k")) } returns StoreProductQueryResult(
            isSuccess = true,
            products = emptyList(),
        )

        viewModel().test(this) {
            containerHost.purchase("token_300k")

            expectState { copy(isPreparing = true, error = null) }
            expectState { copy(isPreparing = false, error = "구매 가능한 상품 정보를 찾지 못했습니다.") }
            expectSideEffect(StoreSideEffect.ShowToast(R.string.store_product_info_unavailable_toast))
        }
    }

    @Test
    fun `결제창 실행 요청을 전달하고 실패 결과를 표시한다`() = runTest {
        val product = billingProduct("token_300k")
        coEvery { getPaymentAccountToken() } returns AppResult.Success("account-token")
        coEvery { getBillingProducts(listOf("token_300k")) } returns StoreProductQueryResult(
            isSuccess = true,
            products = listOf(product),
        )

        viewModel().test(this) {
            containerHost.purchase("token_300k")
            expectState { copy(isPreparing = true, error = null) }
            expectSideEffect(StoreSideEffect.LaunchBillingFlow(product, "account-token"))
            containerHost.onBillingFlowResult(BillingOperationResult(isSuccess = false, responseCode = 6))
            expectState { copy(isPreparing = false, error = "결제창을 열 수 없습니다. (코드: 6)") }
            expectSideEffect(StoreSideEffect.ShowToast(R.string.store_billing_launch_fail_toast))
        }
    }

    @Test
    fun `결제창 실행에 성공하면 구매 준비 상태만 해제하고 안내하지 않는다`() = runTest {
        val product = billingProduct("token_300k")
        coEvery { getPaymentAccountToken() } returns AppResult.Success("account-token")
        coEvery { getBillingProducts(listOf("token_300k")) } returns StoreProductQueryResult(
            isSuccess = true,
            products = listOf(product),
        )

        viewModel().test(this) {
            containerHost.purchase("token_300k")
            expectState { copy(isPreparing = true, error = null) }
            expectSideEffect(StoreSideEffect.LaunchBillingFlow(product, "account-token"))
            containerHost.onBillingFlowResult(BillingOperationResult(isSuccess = true, responseCode = 0))
            expectState { copy(isPreparing = false, error = null) }
            expectNoItems()
        }
    }

    @Test
    fun `사용자가 결제창을 취소하면 오류 안내 없이 구매 중 상태만 해제한다`() = runTest {
        viewModel().test(this) {
            containerHost.onBillingFlowResult(
                BillingOperationResult(
                    isSuccess = false,
                    responseCode = 1,
                    status = BillingResultStatus.USER_CANCELED,
                ),
            )
            expectNoItems()
        }
        verify(exactly = 0) { paymentVerifier.requestRecovery() }
    }

    @Test
    fun `이미 보유한 상품이면 오류 안내 없이 미처리 구매 복구를 요청한다`() = runTest {
        viewModel().test(this) {
            containerHost.onBillingFlowResult(
                BillingOperationResult(
                    isSuccess = false,
                    responseCode = 7,
                    status = BillingResultStatus.ITEM_ALREADY_OWNED,
                ),
            )
            runCurrent()
            expectNoItems()
        }
        verify(exactly = 1) { paymentVerifier.requestRecovery() }
    }

    @Test
    fun `상점 진입 전에 쌓인 결제 결과 안내도 진입 시 화면 상태로 전달한다`() = runTest {
        val notice = PaymentNotice(id = 0, event = PaymentEvent.Granted(grantedTokens = 300_000))
        paymentState.value = PaymentState(notices = listOf(notice))

        viewModel().test(this) {
            containerHost.requestRecovery()
            expectState { copy(payment = PaymentState(notices = listOf(notice))) }
            containerHost.stopObservingPayment()
        }
    }

    @Test
    fun `안내를 표시하면 결제 상태에서 제거하도록 전달한다`() = runTest {
        viewModel().test(this) {
            containerHost.onPaymentNoticeShown(3)
        }
        verify(exactly = 1) { paymentVerifier.onNoticeShown(3) }
    }

    @Test
    fun `결제 결과 안내는 원인별 문구를 쓰고 일시적 실패만 다시 시도하라고 안내한다`() {
        val expected = mapOf(
            PaymentEvent.Granted(300_000) to R.string.store_purchase_complete_toast,
            PaymentEvent.PurchaseFailed to R.string.store_purchase_failed_toast,
            PaymentEvent.VerificationFailed(PaymentFailureReason.RETRYABLE) to
                R.string.store_purchase_verify_fail_toast,
            PaymentEvent.VerificationFailed(PaymentFailureReason.LOGIN_REQUIRED) to
                R.string.store_purchase_login_required_toast,
            PaymentEvent.VerificationFailed(PaymentFailureReason.ACCOUNT_MISMATCH) to
                R.string.store_purchase_account_mismatch_toast,
            PaymentEvent.VerificationFailed(PaymentFailureReason.WITHDRAWN_MEMBER) to
                R.string.store_purchase_withdrawn_member_toast,
            PaymentEvent.VerificationFailed(PaymentFailureReason.PURCHASE_CANCELLED) to
                R.string.store_purchase_cancelled_toast,
            PaymentEvent.VerificationFailed(PaymentFailureReason.PRODUCT_UNAVAILABLE) to
                R.string.store_purchase_product_unavailable_toast,
            PaymentEvent.VerificationFailed(PaymentFailureReason.INVALID_PURCHASE) to
                R.string.store_purchase_invalid_toast,
        )
        expected.forEach { (event, messageRes) -> assertEquals(messageRes, event.messageRes()) }
    }

    private fun billingProduct(productId: String): StoreProduct = mockk {
        every { this@mockk.productId } returns productId
    }

    private fun storeTokenProduct(
        product: TokenProduct,
        billingProduct: StoreProduct?,
        index: Int = 0,
    ): StoreTokenProduct {
        val icon = TokenProductIcons[index]
        return StoreTokenProduct(
            product = product,
            billingProduct = billingProduct,
            labelAmountInTenThousands = product.labelAmountInTenThousands(),
            iconRes = icon.iconRes,
            iconSize = icon.iconSize,
        )
    }
}
